import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, useFieldArray } from 'react-hook-form';
import type { Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, AlertCircle, Plus, Trash2 } from 'lucide-react';
import { AnimatePresence, motion } from 'motion/react';
import { listsApi } from '../services/api';
import { ItemListFormData, FIELD_TYPE_OPTIONS, FIELD_TYPE_LABELS, CustomFieldType } from '../types/item';
import { listFormSchema } from '../schemas/item.schemas';
import { Skeleton, SkeletonText } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { BlurFade } from '@/components/effects/blur-fade';

const PASTEL_BORDERS = [
  'border-l-peach',
  'border-l-status-verify',
  'border-l-status-pending',
  'border-l-status-ready',
  'border-l-status-prepare',
];

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
      showToast('Echec du chargement de la liste', 'error');
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
        showToast('Liste mise a jour avec succes', 'success');
        navigate(`/lists/${id}`);
      } else {
        const newList = await listsApi.create(data);
        showToast('Liste creee avec succes', 'success');
        navigate(`/lists/${newList.id}`);
      }
    } catch (error) {
      console.error('Failed to save list:', error);
      showToast("Echec de l'enregistrement. Veuillez reessayer.", 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto">
        <SkeletonText className="w-36 h-5 mb-8" />
        <SkeletonText className="w-48 h-10 mb-10" />
        <div className="space-y-8">
          {[...Array(3)].map((_, i) => (
            <div key={i}>
              <SkeletonText className="w-20 h-4 mb-2" />
              <Skeleton className="w-full h-10 rounded-lg" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto animate-fade-in">
      <button
        onClick={() => navigate(isEditing && id ? `/lists/${id}` : '/lists')}
        className="inline-flex items-center text-muted-foreground hover:text-foreground mb-8 transition-all duration-200 hover:-translate-x-0.5 group text-sm"
      >
        <ArrowLeft className="h-4 w-4 mr-1.5 transition-transform group-hover:-translate-x-0.5" />
        {isEditing ? 'Retour a la liste' : 'Retour aux listes'}
      </button>

      <BlurFade>
        <h1 className="font-display text-4xl font-semibold tracking-tight mb-2">
          {isEditing ? 'Modifier la liste' : 'Nouvelle liste'}
        </h1>
      </BlurFade>
      <Separator className="my-8" />

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
        <div className="space-y-2">
          <Label htmlFor="list-name">Nom *</Label>
          <Input
            id="list-name"
            type="text"
            {...register('name')}
            className={errors.name ? 'border-destructive focus-visible:ring-destructive' : ''}
            placeholder="Entrez le nom de la liste"
          />
          {errors.name && (
            <p className="text-sm text-destructive flex items-center">
              <AlertCircle className="h-4 w-4 mr-1.5" />
              {errors.name.message}
            </p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="list-description">Description</Label>
          <Textarea
            id="list-description"
            {...register('description')}
            rows={3}
            placeholder="Entrez une description (optionnel)"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="list-category">Categorie</Label>
          <Input
            id="list-category"
            type="text"
            {...register('category')}
            placeholder="Entrez la categorie (optionnel)"
          />
        </div>

        {/* Custom Field Definitions */}
        <div>
          <Label className="mb-3 block">Champs personnalises</Label>

          <AnimatePresence mode="popLayout">
            {fields.map((field, index) => (
              <motion.div
                key={field.id}
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.2 }}
                className="mb-3"
              >
                <div
                  className={`flex gap-3 items-start p-4 rounded-xl border bg-card border-l-4 ${PASTEL_BORDERS[index % PASTEL_BORDERS.length]}`}
                >
                  <div className="flex-1 min-w-0">
                    <Input
                      type="text"
                      {...register(`customFieldDefinitions.${index}.label`)}
                      aria-label={`Libelle du champ ${index + 1}`}
                      placeholder="Libelle du champ"
                      className="h-9"
                    />
                  </div>

                  <div className="w-32">
                    <select
                      {...register(`customFieldDefinitions.${index}.type`)}
                      aria-label={`Type du champ ${index + 1}`}
                      className="flex h-9 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
                      className="rounded border-input"
                    />
                    <span className="text-xs text-muted-foreground">Requis</span>
                  </label>

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="h-9 w-9 text-muted-foreground hover:text-destructive shrink-0"
                    onClick={() => remove(index)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>

          <Button
            type="button"
            variant="outline"
            size="sm"
            className="border-dashed"
            onClick={() =>
              append({
                name: '',
                label: '',
                type: 'TEXT',
                required: false,
                displayOrder: fields.length,
              })
            }
          >
            <Plus className="h-4 w-4 mr-1.5" />
            Ajouter un champ
          </Button>
        </div>

        <Separator />

        <div className="flex gap-4">
          <Button
            type="button"
            variant="ghost"
            className="flex-1"
            onClick={() => navigate(isEditing && id ? `/lists/${id}` : '/lists')}
          >
            Annuler
          </Button>
          <Button
            type="submit"
            disabled={submitting}
            className="flex-1"
          >
            {submitting ? 'Enregistrement...' : isEditing ? 'Mettre a jour' : 'Creer'}
          </Button>
        </div>
      </form>
    </div>
  );
}
