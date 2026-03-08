import { useEffect, useState, useMemo, useRef, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import type { Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Upload, AlertCircle } from "lucide-react";
import axios from "axios";
import { itemsApi, listsApi } from "../services/api";
import {
  ItemFormData,
  STATUS_OPTIONS,
  STATUS_LABELS,
  getItemImageUrl,
  CustomFieldDefinition,
  FIELD_TYPE_LABELS,
} from "../types/item";
import { createItemSchema } from "../schemas/item.schemas";
import { Skeleton, SkeletonText } from "../components/Skeleton";
import { useToast } from "../components/Toast";
import ListCombobox from "../components/ListCombobox";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { BlurFade } from "@/components/effects/blur-fade";
import { cn } from "@/lib/utils";

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_FILE_TYPES = [
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
];

export default function ItemForm() {
  const { listId, itemId } = useParams();
  const navigate = useNavigate();
  const isEditing = Boolean(itemId);
  const { showToast } = useToast();

  const [loading, setLoading] = useState(isEditing);
  const [submitting, setSubmitting] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [selectedListDefs, setSelectedListDefs] = useState<
    CustomFieldDefinition[]
  >([]);

  const schemaRef = useRef(createItemSchema([]));
  const resolver = useCallback<Resolver<ItemFormData>>(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (...args) => (zodResolver(schemaRef.current) as any)(...args),
    [],
  );

  const {
    register,
    handleSubmit,
    reset,
    watch,
    control,
    setValue,
    formState: { errors },
  } = useForm<ItemFormData>({
    resolver,
    defaultValues: {
      name: "",
      itemListId: listId || "",
      status: "IN_STOCK",
      stock: 0,
      customFieldValues: {},
    },
  });

  const selectedListId = watch("itemListId");

  const fieldDefs = useMemo<CustomFieldDefinition[]>(() => {
    return [...selectedListDefs].sort(
      (a, b) => a.displayOrder - b.displayOrder,
    );
  }, [selectedListDefs]);

  useEffect(() => {
    schemaRef.current = createItemSchema(fieldDefs);
  }, [fieldDefs]);

  useEffect(() => {
    if (!isEditing || !itemId) {
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    const load = async () => {
      try {
        const item = await itemsApi.getById(itemId, controller.signal);
        if (!controller.signal.aborted) {
          reset({
            name: item.name,
            itemListId: item.itemListId,
            status: item.status,
            stock: item.stock,
            customFieldValues: item.customFieldValues || {},
          });
          if (item.hasImage) {
            setImagePreview(getItemImageUrl(item.id));
          }
        }
      } catch (err) {
        if (!axios.isCancel(err) && !controller.signal.aborted) {
          showToast("Echec du chargement de l'article", "error");
          navigate(listId ? `/lists/${listId}` : "/lists");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };
    load();
    return () => controller.abort();
  }, [itemId, isEditing, reset, navigate, listId, showToast]);

  useEffect(() => {
    if (!selectedListId) {
      setSelectedListDefs([]);
      return;
    }
    const controller = new AbortController();
    listsApi
      .getById(selectedListId, controller.signal)
      .then((list) => {
        if (!controller.signal.aborted)
          setSelectedListDefs(list.customFieldDefinitions || []);
      })
      .catch(() => {
        if (!controller.signal.aborted) setSelectedListDefs([]);
      });
    return () => controller.abort();
  }, [selectedListId]);

  useEffect(() => {
    if (!isEditing) setValue("customFieldValues", {});
  }, [selectedListId, isEditing, setValue]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > MAX_FILE_SIZE) {
        showToast("La taille du fichier doit être inférieure à 10 Mo", "error");
        return;
      }
      if (!ALLOWED_FILE_TYPES.includes(file.type)) {
        showToast(
          "Seules les images JPEG, PNG, GIF et WebP sont autorisées",
          "error",
        );
        return;
      }
      setImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        if (typeof reader.result === "string") setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const onSubmit = async (data: ItemFormData) => {
    setSubmitting(true);
    try {
      const payload: ItemFormData = {
        ...data,
        customFieldValues:
          fieldDefs.length > 0 ? data.customFieldValues : undefined,
      };
      if (isEditing && itemId) {
        await itemsApi.update(itemId, payload, imageFile || undefined);
        showToast("Article mis à jour avec succès", "success");
      } else {
        await itemsApi.create(payload, imageFile || undefined);
        showToast("Article créé avec succès", "success");
      }
      navigate(`/lists/${data.itemListId}`);
    } catch {
      showToast("Échec de l'enregistrement. Veuillez réessayer.", "error");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto">
        <SkeletonText className="w-36 h-5 mb-8" />
        <SkeletonText className="w-48 h-10 mb-10" />
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
          <div className="lg:col-span-3 space-y-6">
            {[...Array(4)].map((_, i) => (
              <div key={i}>
                <SkeletonText className="w-20 h-4 mb-2" />
                <Skeleton className="w-full h-10 rounded-lg" />
              </div>
            ))}
          </div>
          <div className="lg:col-span-2">
            <Skeleton className="w-full h-64 rounded-xl" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto animate-fade-in">
      <button
        onClick={() => navigate(listId ? `/lists/${listId}` : "/lists")}
        className="inline-flex items-center text-muted-foreground hover:text-foreground mb-8 transition-all duration-200 hover:-translate-x-0.5 group text-sm"
      >
        <ArrowLeft className="h-4 w-4 mr-1.5 transition-transform group-hover:-translate-x-0.5" />
        Retour à la liste
      </button>

      <BlurFade>
        <h1 className="font-display text-4xl font-semibold tracking-tight mb-2">
          {isEditing ? "Modifier l'article" : "Ajouter un article"}
        </h1>
      </BlurFade>
      <Separator className="my-8" />

      <form onSubmit={handleSubmit(onSubmit)}>
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
          {/* Left column - form fields */}
          <div className="lg:col-span-3 space-y-6">
            <div className="space-y-2">
              <Label htmlFor="item-name">Nom *</Label>
              <Input
                id="item-name"
                type="text"
                {...register("name")}
                className={
                  errors.name
                    ? "border-destructive focus-visible:ring-destructive"
                    : ""
                }
                placeholder="Entrez le nom de l'article"
              />
              {errors.name && (
                <p className="text-sm text-destructive flex items-center">
                  <AlertCircle className="h-4 w-4 mr-1.5" />
                  {errors.name.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="item-list">Liste *</Label>
              <Controller
                name="itemListId"
                control={control}
                render={({ field }) => (
                  <ListCombobox
                    id="item-list"
                    value={field.value}
                    onChange={field.onChange}
                    error={Boolean(errors.itemListId)}
                  />
                )}
              />
              {errors.itemListId && (
                <p className="text-sm text-destructive flex items-center">
                  <AlertCircle className="h-4 w-4 mr-1.5" />
                  {errors.itemListId.message}
                </p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="item-stock">Stock</Label>
                <Input
                  id="item-stock"
                  type="number"
                  min="0"
                  {...register("stock", { valueAsNumber: true })}
                  className={
                    errors.stock
                      ? "border-destructive focus-visible:ring-destructive"
                      : ""
                  }
                  placeholder="0"
                />
                {errors.stock && (
                  <p className="text-sm text-destructive flex items-center">
                    <AlertCircle className="h-4 w-4 mr-1.5" />
                    {errors.stock.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="item-status">Statut</Label>
                <select
                  id="item-status"
                  {...register("status")}
                  className="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                >
                  {STATUS_OPTIONS.map((status) => (
                    <option key={status} value={status}>
                      {STATUS_LABELS[status]}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Custom Fields */}
            {fieldDefs.length > 0 && (
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <Separator className="flex-1" />
                  <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                    Champs personnalisés
                  </span>
                  <Separator className="flex-1" />
                </div>
                {fieldDefs.map((def) => {
                  const fieldPath =
                    `customFieldValues.${def.name}` as `customFieldValues.${string}`;
                  const fieldId = `custom-field-${def.name}`;
                  const customErrors = errors.customFieldValues as
                    | Record<string, { message?: string }>
                    | undefined;
                  const fieldError = customErrors?.[def.name];

                  return (
                    <div key={def.name} className="space-y-2">
                      <Label htmlFor={fieldId}>
                        {def.label} {def.required && "*"}{" "}
                        <span className="text-muted-foreground font-normal">
                          ({FIELD_TYPE_LABELS[def.type]})
                        </span>
                      </Label>

                      {def.type === "TEXT" && (
                        <Input
                          id={fieldId}
                          type="text"
                          // eslint-disable-next-line @typescript-eslint/no-explicit-any
                          {...register(fieldPath as any)}
                          className={
                            fieldError
                              ? "border-destructive focus-visible:ring-destructive"
                              : ""
                          }
                          placeholder={`Entrez ${def.label.toLowerCase()}`}
                        />
                      )}

                      {def.type === "NUMBER" && (
                        <Input
                          id={fieldId}
                          type="number"
                          step="any"
                          // eslint-disable-next-line @typescript-eslint/no-explicit-any
                          {...register(fieldPath as any, {
                            valueAsNumber: true,
                          })}
                          className={
                            fieldError
                              ? "border-destructive focus-visible:ring-destructive"
                              : ""
                          }
                          placeholder="0"
                        />
                      )}

                      {def.type === "DATE" && (
                        <Input
                          id={fieldId}
                          type="date"
                          // eslint-disable-next-line @typescript-eslint/no-explicit-any
                          {...register(fieldPath as any)}
                          className={
                            fieldError
                              ? "border-destructive focus-visible:ring-destructive"
                              : ""
                          }
                        />
                      )}

                      {def.type === "BOOLEAN" && (
                        <Controller
                          // eslint-disable-next-line @typescript-eslint/no-explicit-any
                          name={fieldPath as any}
                          control={control}
                          defaultValue={false}
                          render={({ field }) => (
                            <label
                              htmlFor={fieldId}
                              className="flex items-center gap-3 h-10 px-3 border rounded-lg cursor-pointer hover:bg-secondary/50 transition-colors"
                            >
                              <input
                                id={fieldId}
                                type="checkbox"
                                checked={Boolean(field.value)}
                                onChange={(e) =>
                                  field.onChange(e.target.checked)
                                }
                                className="rounded border-input"
                              />
                              <span className="text-sm">{def.label}</span>
                            </label>
                          )}
                        />
                      )}

                      {def.type === "SELECT" && (
                        <select
                          id={fieldId}
                          // eslint-disable-next-line @typescript-eslint/no-explicit-any
                          {...register(fieldPath as any)}
                          className={cn(
                            "flex h-10 w-full rounded-lg border bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
                            fieldError ? "border-destructive" : "border-input",
                          )}
                        >
                          <option value="">-- Choisir --</option>
                          {def.options?.map((opt) => (
                            <option key={opt} value={opt}>
                              {opt}
                            </option>
                          ))}
                        </select>
                      )}

                      {fieldError && (
                        <p className="text-sm text-destructive flex items-center">
                          <AlertCircle className="h-4 w-4 mr-1.5" />
                          {fieldError.message}
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Right column - image upload */}
          <div className="lg:col-span-2">
            <Label className="mb-2 block">Image</Label>
            <div className="sticky top-8">
              <label className="block cursor-pointer">
                <div
                  className={cn(
                    "flex flex-col items-center justify-center rounded-2xl border-2 border-dashed transition-all duration-200 hover:border-brand-dark hover:bg-brand-light/30 group min-h-[280px] overflow-hidden",
                    imagePreview
                      ? "border-transparent p-0"
                      : "border-border p-8",
                  )}
                >
                  {imagePreview ? (
                    <div className="relative w-full h-full min-h-[280px]">
                      <img
                        src={imagePreview}
                        alt="Apercu"
                        className="w-full h-full object-cover rounded-2xl"
                      />
                      <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity rounded-2xl flex items-center justify-center">
                        <span className="text-white text-sm font-medium">
                          Changer l'image
                        </span>
                      </div>
                    </div>
                  ) : (
                    <>
                      <Upload className="h-10 w-10 text-muted-foreground/50 mb-3 group-hover:text-brand-dark group-hover:scale-110 transition-all duration-200" />
                      <span className="text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors">
                        Télécharger une image
                      </span>
                      <p className="text-xs text-muted-foreground mt-1">
                        PNG, JPG, GIF jusqu'à 10 Mo
                      </p>
                    </>
                  )}
                </div>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  className="sr-only"
                />
              </label>
            </div>
          </div>
        </div>

        <Separator className="my-8" />

        <div className="flex gap-4 max-w-2xl">
          <Button
            type="button"
            variant="ghost"
            className="flex-1"
            onClick={() => navigate(listId ? `/lists/${listId}` : "/lists")}
          >
            Annuler
          </Button>
          <Button type="submit" disabled={submitting} className="flex-1">
            {submitting
              ? "Enregistrement..."
              : isEditing
                ? "Mettre à jour"
                : "Ajouter"}
          </Button>
        </div>
      </form>
    </div>
  );
}
