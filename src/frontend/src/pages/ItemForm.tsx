import { useEffect, useState, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { ArrowLeft, Upload, AlertCircle } from 'lucide-react';
import { itemsApi, listsApi } from '../services/api';
import { ItemFormData, STATUS_OPTIONS, STATUS_LABELS, ItemList, getItemImageUrl, CustomFieldDefinition, FIELD_TYPE_LABELS, CustomFieldType } from '../types/item';
import { Skeleton, SkeletonText } from '../components/Skeleton';
import { useToast } from '../components/Toast';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_FILE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

export default function ItemForm() {
  const { listId, itemId } = useParams();
  const navigate = useNavigate();
  const isEditing = Boolean(itemId);
  const { showToast } = useToast();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [lists, setLists] = useState<ItemList[]>([]);
  const [customValues, setCustomValues] = useState<Record<string, unknown>>({});

  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm<ItemFormData>({
    defaultValues: {
      name: '',
      itemListId: listId || '',
      status: 'TO_PREPARE',
      stock: 0,
    },
  });

  const selectedListId = watch('itemListId');

  const fieldDefs = useMemo<CustomFieldDefinition[]>(() => {
    const selectedList = lists.find((l) => l.id === selectedListId);
    const defs = selectedList?.customFieldDefinitions || [];
    return [...defs].sort((a, b) => a.displayOrder - b.displayOrder);
  }, [lists, selectedListId]);

  useEffect(() => {
    loadLists();
  }, []);

  useEffect(() => {
    if (isEditing && itemId) {
      loadItem(itemId);
    } else if (lists.length > 0) {
      setLoading(false);
    }
  }, [itemId, isEditing, lists]);

  // Reset custom values when switching lists (only for new items)
  useEffect(() => {
    if (!isEditing && selectedListId) {
      setCustomValues({});
    }
  }, [selectedListId, isEditing]);

  const loadLists = async () => {
    try {
      const response = await listsApi.getAll({ size: 100 });
      setLists(response.content);
    } catch (error) {
      console.error('Failed to load lists:', error);
      showToast('Échec du chargement des listes', 'error');
    }
  };

  const loadItem = async (id: string) => {
    try {
      const item = await itemsApi.getById(id);
      reset({
        name: item.name,
        itemListId: item.itemListId,
        status: item.status,
        stock: item.stock,
      });
      setCustomValues(item.customFieldValues || {});
      if (item.hasImage) {
        setImagePreview(getItemImageUrl(item.id));
      }
    } catch (error) {
      console.error('Failed to load item:', error);
      showToast('Échec du chargement de l\'article', 'error');
      navigate(listId ? `/lists/${listId}` : '/lists');
    } finally {
      setLoading(false);
    }
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > MAX_FILE_SIZE) {
        showToast('La taille du fichier doit être inférieure à 10 Mo', 'error');
        return;
      }
      if (!ALLOWED_FILE_TYPES.includes(file.type)) {
        showToast('Seules les images JPEG, PNG, GIF et WebP sont autorisées', 'error');
        return;
      }
      setImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleCustomFieldChange = (name: string, type: CustomFieldType, rawValue: string | boolean) => {
    setCustomValues((prev) => {
      const next = { ...prev };
      if (type === 'NUMBER') {
        const num = parseFloat(rawValue as string);
        next[name] = isNaN(num) ? null : num;
      } else if (type === 'BOOLEAN') {
        next[name] = rawValue as boolean;
      } else {
        next[name] = rawValue;
      }
      return next;
    });
  };

  const onSubmit = async (data: ItemFormData) => {
    setSubmitting(true);
    try {
      const payload: ItemFormData = {
        ...data,
        customFieldValues: fieldDefs.length > 0 ? customValues : undefined,
      };

      if (isEditing && itemId) {
        await itemsApi.update(itemId, payload, imageFile || undefined);
        showToast('Article mis à jour avec succès', 'success');
      } else {
        await itemsApi.create(payload, imageFile || undefined);
        showToast('Article créé avec succès', 'success');
      }
      navigate(`/lists/${data.itemListId}`);
    } catch (error) {
      console.error('Failed to save item:', error);
      showToast('Échec de l\'enregistrement. Veuillez réessayer.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto">
        <SkeletonText className="w-36 h-5 mb-6" />
        <div className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 rounded-2xl border border-white/[0.06] shadow-premium p-8">
          <SkeletonText className="w-40 h-9 mb-8" />
          <div className="space-y-6">
            {[...Array(5)].map((_, i) => (
              <div key={i}>
                <SkeletonText className="w-20 h-4 mb-2" />
                <Skeleton className="w-full h-12 rounded-xl" />
              </div>
            ))}
            <div className="flex gap-4 pt-2">
              <Skeleton className="flex-1 h-12 rounded-xl" />
              <Skeleton className="flex-1 h-12 rounded-xl" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto animate-fade-in">
      <button
        onClick={() => navigate(listId ? `/lists/${listId}` : '/lists')}
        className="inline-flex items-center text-stone-400 hover:text-stone-200 mb-6 transition-all duration-200 hover:-translate-x-0.5 group"
      >
        <ArrowLeft className="h-5 w-5 mr-1.5 transition-transform group-hover:-translate-x-0.5" />
        Retour à la liste
      </button>

      <div className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 backdrop-blur-xl rounded-2xl border border-white/[0.06] shadow-premium p-8">
        <h1 className="font-display text-3xl text-stone-100 mb-8 tracking-tight">
          {isEditing ? 'Modifier l\'article' : 'Ajouter un article'}
        </h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div>
            <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
              Nom *
            </label>
            <input
              type="text"
              {...register('name', { required: 'Le nom est requis' })}
              className={`w-full px-4 py-3 bg-surface-base/50 border rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 hover:border-white/[0.12] ${
                errors.name
                  ? 'border-red-400/40 focus:ring-red-400/30 focus:border-red-400/50'
                  : 'border-white/[0.08] focus:ring-amber-500/30 focus:border-amber-500/40'
              }`}
              placeholder="Entrez le nom de l'article"
            />
            {errors.name && (
              <p className="mt-2 text-sm text-red-400 flex items-center">
                <AlertCircle className="h-4 w-4 mr-1.5" />
                {errors.name.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
              Liste *
            </label>
            <select
              {...register('itemListId', { required: 'La liste est requise' })}
              className={`w-full px-4 py-3 bg-surface-base/50 border rounded-xl text-stone-100 cursor-pointer transition-all duration-200 focus:outline-none focus:ring-2 hover:border-white/[0.12] ${
                errors.itemListId
                  ? 'border-red-400/40 focus:ring-red-400/30 focus:border-red-400/50'
                  : 'border-white/[0.08] focus:ring-amber-500/30 focus:border-amber-500/40'
              }`}
            >
              <option value="">Sélectionnez une liste</option>
              {lists.map((list) => (
                <option key={list.id} value={list.id}>{list.name}</option>
              ))}
            </select>
            {errors.itemListId && (
              <p className="mt-2 text-sm text-red-400 flex items-center">
                <AlertCircle className="h-4 w-4 mr-1.5" />
                {errors.itemListId.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
              Stock
            </label>
            <input
              type="number"
              min="0"
              {...register('stock', { valueAsNumber: true, min: { value: 0, message: 'Le stock ne peut pas être négatif' } })}
              className={`w-full px-4 py-3 bg-surface-base/50 border rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 hover:border-white/[0.12] ${
                errors.stock
                  ? 'border-red-400/40 focus:ring-red-400/30 focus:border-red-400/50'
                  : 'border-white/[0.08] focus:ring-amber-500/30 focus:border-amber-500/40'
              }`}
              placeholder="0"
            />
            {errors.stock && (
              <p className="mt-2 text-sm text-red-400 flex items-center">
                <AlertCircle className="h-4 w-4 mr-1.5" />
                {errors.stock.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
              Statut
            </label>
            <select
              {...register('status')}
              className="w-full px-4 py-3 bg-surface-base/50 border border-white/[0.08] rounded-xl text-stone-100 cursor-pointer transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40 hover:border-white/[0.12]"
            >
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>{STATUS_LABELS[status]}</option>
              ))}
            </select>
          </div>

          {/* Custom Fields */}
          {fieldDefs.length > 0 && (
            <div className="space-y-4 pt-2">
              <div className="border-t border-white/[0.06] pt-4">
                <p className="text-xs font-medium text-stone-400 mb-4 uppercase tracking-wider">
                  Champs personnalisés
                </p>
              </div>
              {fieldDefs.map((def) => (
                <div key={def.name}>
                  <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
                    {def.label} {def.required && '*'} <span className="text-stone-600 normal-case">({FIELD_TYPE_LABELS[def.type]})</span>
                  </label>

                  {def.type === 'TEXT' && (
                    <input
                      type="text"
                      value={(customValues[def.name] as string) || ''}
                      onChange={(e) => handleCustomFieldChange(def.name, def.type, e.target.value)}
                      className="w-full px-4 py-3 bg-surface-base/50 border border-white/[0.08] rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40 hover:border-white/[0.12]"
                      placeholder={`Entrez ${def.label.toLowerCase()}`}
                    />
                  )}

                  {def.type === 'NUMBER' && (
                    <input
                      type="number"
                      step="any"
                      value={customValues[def.name] != null ? String(customValues[def.name]) : ''}
                      onChange={(e) => handleCustomFieldChange(def.name, def.type, e.target.value)}
                      className="w-full px-4 py-3 bg-surface-base/50 border border-white/[0.08] rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40 hover:border-white/[0.12]"
                      placeholder="0"
                    />
                  )}

                  {def.type === 'DATE' && (
                    <input
                      type="date"
                      value={(customValues[def.name] as string) || ''}
                      onChange={(e) => handleCustomFieldChange(def.name, def.type, e.target.value)}
                      className="w-full px-4 py-3 bg-surface-base/50 border border-white/[0.08] rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40 hover:border-white/[0.12]"
                    />
                  )}

                  {def.type === 'BOOLEAN' && (
                    <label className="flex items-center gap-3 px-4 py-3 bg-surface-base/50 border border-white/[0.08] rounded-xl transition-all duration-200 hover:border-white/[0.12] cursor-pointer">
                      <input
                        type="checkbox"
                        checked={(customValues[def.name] as boolean) || false}
                        onChange={(e) => handleCustomFieldChange(def.name, def.type, e.target.checked)}
                        className="rounded border-white/[0.15] bg-surface-base/50 text-amber-500 focus:ring-amber-500/30 focus:ring-offset-0"
                      />
                      <span className="text-stone-300 text-sm">{def.label}</span>
                    </label>
                  )}
                </div>
              ))}
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
              Image
            </label>
            <div className="mt-1 flex justify-center px-6 pt-6 pb-6 border-2 border-white/[0.08] border-dashed rounded-xl bg-surface-base/30 transition-all duration-200 hover:border-amber-500/30 hover:bg-surface-base/50 group cursor-pointer">
              <div className="space-y-3 text-center">
                {imagePreview ? (
                  <div className="mb-4">
                    <img
                      src={imagePreview}
                      alt="Aperçu"
                      className="mx-auto h-32 w-32 object-cover rounded-xl ring-2 ring-white/[0.1]"
                    />
                  </div>
                ) : (
                  <Upload className="mx-auto h-12 w-12 text-stone-500 transition-all duration-200 group-hover:scale-110 group-hover:text-amber-400" />
                )}
                <div className="flex text-sm text-stone-400 justify-center">
                  <label className="relative cursor-pointer rounded-md font-medium text-amber-400 hover:text-amber-300 transition-colors">
                    <span>{imagePreview ? 'Changer l\'image' : 'Télécharger une image'}</span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleImageChange}
                      className="sr-only"
                    />
                  </label>
                </div>
                <p className="text-xs text-stone-500">PNG, JPG, GIF jusqu'à 10 Mo</p>
              </div>
            </div>
          </div>

          <div className="flex gap-4 pt-4">
            <button
              type="button"
              onClick={() => navigate(listId ? `/lists/${listId}` : '/lists')}
              className="flex-1 px-5 py-3 bg-white/[0.04] text-stone-300 font-medium rounded-xl border border-white/[0.08] transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] hover:text-stone-100"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 px-5 py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-surface-base font-semibold rounded-xl shadow-glow-amber transition-all duration-200 hover:from-amber-400 hover:to-amber-500 hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
            >
              {submitting ? 'Enregistrement...' : isEditing ? 'Mettre à jour' : 'Ajouter'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
