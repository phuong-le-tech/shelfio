import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, useFieldArray } from 'react-hook-form';
import type { Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, AlertCircle, Plus, Trash2 } from 'lucide-react';
import { listsApi } from '../services/api';
import { ItemListFormData, FIELD_TYPE_OPTIONS, FIELD_TYPE_LABELS, CustomFieldType } from '../types/item';
import { listFormSchema } from '../schemas/item.schemas';
import { Skeleton, SkeletonText } from '../components/Skeleton';
import { useToast } from '../components/Toast';

function slugify(label: string): string {
  return label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
}

export default function ListForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditing = Boolean(id);
  const { showToast } = useToast();

  const [loading, setLoading] = useState(isEditing);
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, reset, control, formState: { errors } } = useForm<ItemListFormData>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(listFormSchema) as unknown as Resolver<ItemListFormData>,
    defaultValues: {
      name: '',
      description: '',
      category: '',
      customFieldDefinitions: [],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'customFieldDefinitions',
  });

  useEffect(() => {
    if (isEditing && id) {
      loadList(id);
    }
  }, [id, isEditing]);

  const loadList = async (listId: string) => {
    try {
      const list = await listsApi.getById(listId);
      reset({
        name: list.name,
        description: list.description || '',
        category: list.category || '',
        customFieldDefinitions: list.customFieldDefinitions || [],
      });
    } catch (error) {
      console.error('Failed to load list:', error);
      showToast('Échec du chargement de la liste', 'error');
      navigate('/lists');
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = async (data: ItemListFormData) => {
    setSubmitting(true);
    try {
      if (data.customFieldDefinitions) {
        data.customFieldDefinitions = data.customFieldDefinitions.map((def, i) => ({
          ...def,
          name: def.name || slugify(def.label),
          displayOrder: i,
        }));
      }

      if (isEditing && id) {
        await listsApi.update(id, data);
        showToast('Liste mise à jour avec succès', 'success');
        navigate(`/lists/${id}`);
      } else {
        const newList = await listsApi.create(data);
        showToast('Liste créée avec succès', 'success');
        navigate(`/lists/${newList.id}`);
      }
    } catch (error) {
      console.error('Failed to save list:', error);
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
            {[...Array(3)].map((_, i) => (
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
        onClick={() => navigate(isEditing && id ? `/lists/${id}` : '/lists')}
        className="inline-flex items-center text-stone-400 hover:text-stone-200 mb-6 transition-all duration-200 hover:-translate-x-0.5 group"
      >
        <ArrowLeft className="h-5 w-5 mr-1.5 transition-transform group-hover:-translate-x-0.5" />
        {isEditing ? 'Retour à la liste' : 'Retour aux listes'}
      </button>

      <div className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 backdrop-blur-xl rounded-2xl border border-white/[0.06] shadow-premium p-8">
        <h1 className="font-display text-3xl text-stone-100 mb-8 tracking-tight">
          {isEditing ? 'Modifier la liste' : 'Nouvelle liste'}
        </h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div>
            <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
              Nom *
            </label>
            <input
              type="text"
              {...register('name')}
              className={`w-full px-4 py-3 bg-surface-base/50 border rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 hover:border-white/[0.12] ${
                errors.name
                  ? 'border-red-400/40 focus:ring-red-400/30 focus:border-red-400/50'
                  : 'border-white/[0.08] focus:ring-amber-500/30 focus:border-amber-500/40'
              }`}
              placeholder="Entrez le nom de la liste"
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
              Description
            </label>
            <textarea
              {...register('description')}
              rows={3}
              className="w-full px-4 py-3 bg-surface-base/50 border border-white/[0.08] rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40 hover:border-white/[0.12] resize-none"
              placeholder="Entrez une description (optionnel)"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-stone-400 mb-2 uppercase tracking-wider">
              Catégorie
            </label>
            <input
              type="text"
              {...register('category')}
              className="w-full px-4 py-3 bg-surface-base/50 border border-white/[0.08] rounded-xl text-stone-100 placeholder-stone-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40 hover:border-white/[0.12]"
              placeholder="Entrez la catégorie (optionnel)"
            />
          </div>

          {/* Custom Field Definitions */}
          <div>
            <label className="block text-xs font-medium text-stone-400 mb-3 uppercase tracking-wider">
              Champs personnalisés
            </label>

            {fields.length > 0 && (
              <div className="space-y-3 mb-4">
                {fields.map((field, index) => (
                  <div
                    key={field.id}
                    className="flex gap-3 items-start p-3 bg-surface-base/30 border border-white/[0.06] rounded-xl"
                  >
                    <div className="flex-1 min-w-0">
                      <input
                        type="text"
                        {...register(`customFieldDefinitions.${index}.label`)}
                        className="w-full px-3 py-2 bg-surface-base/50 border border-white/[0.08] rounded-lg text-stone-100 placeholder-stone-500 text-sm transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40"
                        placeholder="Libellé du champ"
                      />
                    </div>

                    <div className="w-32">
                      <select
                        {...register(`customFieldDefinitions.${index}.type`)}
                        className="w-full px-3 py-2 bg-surface-base/50 border border-white/[0.08] rounded-lg text-stone-100 text-sm transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40"
                      >
                        {FIELD_TYPE_OPTIONS.map((t) => (
                          <option key={t} value={t}>
                            {FIELD_TYPE_LABELS[t as CustomFieldType]}
                          </option>
                        ))}
                      </select>
                    </div>

                    <label className="flex items-center gap-1.5 pt-2 shrink-0">
                      <input
                        type="checkbox"
                        {...register(`customFieldDefinitions.${index}.required`)}
                        className="rounded border-white/[0.15] bg-surface-base/50 text-amber-500 focus:ring-amber-500/30 focus:ring-offset-0"
                      />
                      <span className="text-xs text-stone-400">Requis</span>
                    </label>

                    <button
                      type="button"
                      onClick={() => remove(index)}
                      className="p-2 text-stone-500 hover:text-red-400 transition-colors shrink-0"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}

            <button
              type="button"
              onClick={() =>
                append({
                  name: '',
                  label: '',
                  type: 'TEXT',
                  required: false,
                  displayOrder: fields.length,
                })
              }
              className="inline-flex items-center gap-1.5 px-4 py-2 text-sm text-stone-400 hover:text-stone-200 bg-white/[0.03] hover:bg-white/[0.06] border border-dashed border-white/[0.1] hover:border-white/[0.15] rounded-xl transition-all duration-200"
            >
              <Plus className="h-4 w-4" />
              Ajouter un champ
            </button>
          </div>

          <div className="flex gap-4 pt-4">
            <button
              type="button"
              onClick={() => navigate(isEditing && id ? `/lists/${id}` : '/lists')}
              className="flex-1 px-5 py-3 bg-white/[0.04] text-stone-300 font-medium rounded-xl border border-white/[0.08] transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] hover:text-stone-100"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 px-5 py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-surface-base font-semibold rounded-xl shadow-glow-amber transition-all duration-200 hover:from-amber-400 hover:to-amber-500 hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
            >
              {submitting ? 'Enregistrement...' : isEditing ? 'Mettre à jour' : 'Créer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
