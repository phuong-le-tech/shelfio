import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, useFieldArray, Controller } from 'react-hook-form';
import type { Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, Plus, Trash2 } from 'lucide-react';
import { AnimatePresence, motion } from 'motion/react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { listsApi } from '../services/api';
import { queryKeys } from '../lib/queryKeys';
import { ItemListFormData, FIELD_TYPE_OPTIONS, FIELD_TYPE_LABELS, CustomFieldType } from '../types/item';
import { listFormSchema } from '../schemas/item.schemas';
import { Skeleton, SkeletonText } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import { useUnsavedChangesGuard } from '../hooks/useUnsavedChangesGuard';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { BlurFade } from '@/components/effects/blur-fade';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { Breadcrumb } from '../components/Breadcrumb';

const PASTEL_BORDERS = [
  'border-l-brand',
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

  const queryClient = useQueryClient();
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, reset, control, watch, formState: { errors, isDirty } } = useForm<ItemListFormData>({
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

  const confirmDiscardChanges = useUnsavedChangesGuard(isDirty && !submitting);

  const { data: listData, isLoading: listLoading, error: listError } = useQuery({
    queryKey: id ? queryKeys.lists.detail(id) : ['lists', 'detail', null],
    queryFn: () => listsApi.getById(id!),
    enabled: isEditing && !!id,
  });

  useEffect(() => {
    if (listData) {
      reset({
        name: listData.name,
        description: listData.description || '',
        category: listData.category || '',
        customFieldDefinitions: listData.customFieldDefinitions || [],
      });
    }
  }, [listData, reset]);

  useEffect(() => {
    if (listError) {
      showToast('Échec du chargement de la liste', 'error');
      navigate('/lists');
    }
  }, [listError, showToast, navigate]);

  const loading = isEditing && listLoading;

  const nameValue = watch('name') ?? '';
  const descriptionValue = watch('description') ?? '';

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
      queryClient.invalidateQueries({ queryKey: queryKeys.lists.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
    } catch {
      showToast("Échec de l'enregistrement. Veuillez réessayer.", 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto">
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
    <div className="max-w-7xl mx-auto animate-fade-in">
      <Breadcrumb
        items={
          isEditing && id
            ? [
                { label: 'Mes Listes', href: '/lists' },
                { label: listData?.name || '...', href: `/lists/${id}` },
                { label: 'Modifier' },
              ]
            : [
                { label: 'Mes Listes', href: '/lists' },
                { label: 'Nouvelle liste' },
              ]
        }
      />

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
            aria-invalid={!!errors.name}
            aria-describedby={errors.name ? 'list-name-error' : undefined}
          />
          <div className="flex justify-between items-center">
            <div>
              {errors.name && (
                <p id="list-name-error" role="alert" className="text-sm text-destructive flex items-center">
                  <AlertCircle className="h-4 w-4 mr-1.5" />
                  {errors.name.message}
                </p>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {(nameValue?.length ?? 0)}/100
            </p>
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="list-description">Description</Label>
          <Textarea
            id="list-description"
            {...register('description')}
            rows={3}
            placeholder="Entrez une description (optionnel)"
          />
          <p className="text-xs text-muted-foreground text-right">
            {(descriptionValue?.length ?? 0)}/500
          </p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="list-category">Catégorie</Label>
          <Input
            id="list-category"
            type="text"
            {...register('category')}
            placeholder="Entrez la catégorie (optionnel)"
          />
        </div>

        {/* Custom Field Definitions */}
        <div>
          <Label className="mb-3 block">Champs personnalisés</Label>

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
                      aria-label={`Libellé du champ ${index + 1}`}
                      placeholder="Libellé du champ"
                      className="h-9"
                    />
                  </div>

                  <div className="w-32">
                    <Controller
                      name={`customFieldDefinitions.${index}.type`}
                      control={control}
                      defaultValue="TEXT"
                      render={({ field }) => (
                        <Select value={field.value} onValueChange={field.onChange}>
                          <SelectTrigger className="h-9" aria-label={`Type du champ ${index + 1}`}>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {FIELD_TYPE_OPTIONS.map((t) => (
                              <SelectItem key={t} value={t}>
                                {FIELD_TYPE_LABELS[t as CustomFieldType]}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </div>

                  <Controller
                    name={`customFieldDefinitions.${index}.required`}
                    control={control}
                    defaultValue={false}
                    render={({ field }) => (
                      <label className="flex items-center gap-1.5 pt-2 shrink-0 cursor-pointer">
                        <Checkbox
                          checked={Boolean(field.value)}
                          onCheckedChange={field.onChange}
                          aria-label={`Champ ${index + 1} requis`}
                        />
                        <span className="text-xs text-muted-foreground">Requis</span>
                      </label>
                    )}
                  />

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
            onClick={() => {
              if (!confirmDiscardChanges()) return;
              navigate(isEditing && id ? `/lists/${id}` : '/lists');
            }}
          >
            Annuler
          </Button>
          <Button
            type="submit"
            disabled={submitting}
            className="flex-1"
          >
            {submitting ? 'Enregistrement...' : isEditing ? 'Mettre à jour' : 'Créer'}
          </Button>
        </div>
      </form>

    </div>
  );
}
