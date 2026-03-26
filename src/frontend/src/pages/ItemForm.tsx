import { useEffect, useState, useMemo, useRef, useCallback } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import type { Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Upload, AlertCircle, X, ScanLine, Sparkles } from "lucide-react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { itemsApi, listsApi } from "../services/api";
import { queryKeys } from "../lib/queryKeys";
import {
  ItemFormData,
  STATUS_OPTIONS,
  STATUS_LABELS,
  CustomFieldDefinition,
  FIELD_TYPE_LABELS,
} from "../types/item";
import { createItemSchema } from "../schemas/item.schemas";
import { Skeleton, SkeletonText } from "../components/Skeleton";
import { useToast } from "../components/Toast";
import ListCombobox from "../components/ListCombobox";
import { useUnsavedChangesGuard } from "../hooks/useUnsavedChangesGuard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { BlurFade } from "@/components/effects/blur-fade";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";
import { Breadcrumb } from "../components/Breadcrumb";
import BarcodeScannerModal from "../components/BarcodeScannerModal";
import { ImageAnalysisSuggestions } from "../components/ImageAnalysisSuggestions";
import { useImageAnalysis } from "../hooks/useImageAnalysis";
import { sanitizeImageUrl } from "../utils/imageUtils";

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_FILE_TYPES = [
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
];

export default function ItemForm() {
  const { listId, itemId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const isEditing = Boolean(itemId);
  const { showToast } = useToast();

  const queryClient = useQueryClient();
  const [submitting, setSubmitting] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [selectedListDefs, setSelectedListDefs] = useState<
    CustomFieldDefinition[]
  >([]);
  const [selectedListName, setSelectedListName] = useState("");
  const [scannerOpen, setScannerOpen] = useState(false);
  const { suggestions, isAnalyzing, isAvailable: aiAvailable, startAnalysis, reset: resetAnalysis } = useImageAnalysis();

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
    formState: { errors, isDirty },
  } = useForm<ItemFormData>({
    resolver,
    defaultValues: {
      name: "",
      itemListId: listId || "",
      status: "AVAILABLE",
      stock: 0,
      barcode: searchParams.get("barcode") || "",
      customFieldValues: {},
    },
  });

  const confirmDiscardChanges = useUnsavedChangesGuard(isDirty && !submitting);

  const selectedListId = watch("itemListId");
  const nameValue = watch("name") ?? "";

  const fieldDefs = useMemo<CustomFieldDefinition[]>(() => {
    return [...selectedListDefs].sort(
      (a, b) => a.displayOrder - b.displayOrder,
    );
  }, [selectedListDefs]);

  useEffect(() => {
    schemaRef.current = createItemSchema(fieldDefs);
  }, [fieldDefs]);

  const {
    data: itemData,
    isLoading: itemLoading,
    error: itemError,
  } = useQuery({
    queryKey: itemId
      ? queryKeys.items.detail(itemId)
      : ["items", "detail", null],
    queryFn: () => itemsApi.getById(itemId!),
    enabled: isEditing && !!itemId,
  });

  useEffect(() => {
    if (itemData) {
      reset({
        name: itemData.name,
        itemListId: itemData.itemListId,
        status: itemData.status,
        stock: itemData.stock,
        barcode: itemData.barcode || "",
        customFieldValues: itemData.customFieldValues || {},
      });
      const safeUrl = sanitizeImageUrl(itemData.imageUrl);
      if (safeUrl) {
        setImagePreview(safeUrl);
      }
    }
  }, [itemData, reset]);

  useEffect(() => {
    if (itemError) {
      showToast("Échec du chargement de l'article", "error");
      navigate(listId ? `/lists/${listId}` : "/lists");
    }
  }, [itemError, showToast, navigate, listId]);

  const loading = isEditing && itemLoading;

  useEffect(() => {
    if (!selectedListId) {
      setSelectedListDefs([]);
      setSelectedListName("");
      return;
    }
    const controller = new AbortController();
    listsApi
      .getById(selectedListId, controller.signal)
      .then((list) => {
        if (!controller.signal.aborted) {
          setSelectedListDefs(list.customFieldDefinitions || []);
          setSelectedListName(list.name);
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) setSelectedListDefs([]);
      });
    return () => controller.abort();
  }, [selectedListId]);

  useEffect(() => {
    if (!isEditing) setValue("customFieldValues", {});
  }, [selectedListId, isEditing, setValue]);

  const processImageFile = (file: File) => {
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
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      processImageFile(file);
    }
  };

  const handleRemoveImage = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setImageFile(null);
    setImagePreview(null);
    resetAnalysis();
  };

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const file = e.dataTransfer.files?.[0];
    if (file) {
      processImageFile(file);
    }
  };

  const handleApplySuggestion = useCallback((field: string, value: unknown) => {
    // Whitelist custom field keys against known definitions to prevent prototype pollution
    if (field.startsWith('customFieldValues.')) {
      const key = field.replace('customFieldValues.', '');
      if (!fieldDefs.some(f => f.name === key)) return;
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    setValue(field as any, value, { shouldDirty: true });
  }, [setValue, fieldDefs]);

  const handleApplyAllSuggestions = useCallback(() => {
    if (!suggestions) return;
    if (suggestions.suggestedName) setValue('name', suggestions.suggestedName, { shouldDirty: true });
    if (suggestions.suggestedStatus) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      setValue('status', suggestions.suggestedStatus as any, { shouldDirty: true });
    }
    if (suggestions.suggestedStock != null) setValue('stock', suggestions.suggestedStock, { shouldDirty: true });
    if (suggestions.suggestedCustomFieldValues) {
      const knownKeys = new Set(fieldDefs.map(f => f.name));
      for (const [key, val] of Object.entries(suggestions.suggestedCustomFieldValues)) {
        if (!knownKeys.has(key)) continue; // ignore unknown/injected keys
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        setValue(`customFieldValues.${key}` as any, val, { shouldDirty: true });
      }
    }
    resetAnalysis();
  }, [suggestions, setValue, resetAnalysis, fieldDefs]);

  const onSubmit = async (data: ItemFormData) => {
    resetAnalysis();
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
      queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
      queryClient.invalidateQueries({
        queryKey: queryKeys.lists.detail(data.itemListId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
    } catch {
      showToast("Échec de l'enregistrement. Veuillez réessayer.", "error");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto">
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
    <div className="max-w-7xl mx-auto animate-fade-in">
      <Breadcrumb
        items={[
          { label: "Mes Listes", href: "/lists" },
          ...(listId
            ? [{ label: selectedListName || "...", href: `/lists/${listId}` }]
            : []),
          { label: isEditing ? "Modifier" : "Nouvel article" },
        ]}
      />

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
                aria-invalid={!!errors.name}
                aria-describedby={errors.name ? "item-name-error" : undefined}
              />
              <div className="flex justify-between items-center">
                <div>
                  {errors.name && (
                    <p
                      id="item-name-error"
                      role="alert"
                      className="text-sm text-destructive flex items-center"
                    >
                      <AlertCircle className="h-4 w-4 mr-1.5" />
                      {errors.name.message}
                    </p>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  {nameValue?.length ?? 0}/200
                </p>
              </div>
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
                    aria-invalid={!!errors.itemListId}
                    aria-describedby={
                      errors.itemListId ? "item-list-error" : undefined
                    }
                  />
                )}
              />
              {errors.itemListId && (
                <p
                  id="item-list-error"
                  role="alert"
                  className="text-sm text-destructive flex items-center"
                >
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
                  aria-invalid={!!errors.stock}
                  aria-describedby={
                    errors.stock ? "item-stock-error" : undefined
                  }
                />
                {errors.stock && (
                  <p
                    id="item-stock-error"
                    role="alert"
                    className="text-sm text-destructive flex items-center"
                  >
                    <AlertCircle className="h-4 w-4 mr-1.5" />
                    {errors.stock.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="item-status">Statut</Label>
                <Controller
                  name="status"
                  control={control}
                  defaultValue="AVAILABLE"
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger id="item-status">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {STATUS_OPTIONS.map((status) => (
                          <SelectItem key={status} value={status}>
                            {STATUS_LABELS[status]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="item-barcode">Code-barres</Label>
              <div className="flex gap-2">
                <Input
                  id="item-barcode"
                  type="text"
                  {...register("barcode")}
                  className={cn(
                    "flex-1",
                    errors.barcode
                      ? "border-destructive focus-visible:ring-destructive"
                      : "",
                  )}
                  placeholder="Scannez ou entrez un code-barres"
                  aria-invalid={!!errors.barcode}
                  aria-describedby={
                    errors.barcode ? "item-barcode-error" : undefined
                  }
                />
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  onClick={() => setScannerOpen(true)}
                  title="Scanner un code"
                >
                  <ScanLine className="h-4 w-4" />
                </Button>
              </div>
              {errors.barcode && (
                <p
                  id="item-barcode-error"
                  role="alert"
                  className="text-sm text-destructive flex items-center"
                >
                  <AlertCircle className="h-4 w-4 mr-1.5" />
                  {errors.barcode.message}
                </p>
              )}
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
                          aria-invalid={!!fieldError}
                          aria-describedby={
                            fieldError ? `${fieldId}-error` : undefined
                          }
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
                          aria-invalid={!!fieldError}
                          aria-describedby={
                            fieldError ? `${fieldId}-error` : undefined
                          }
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
                          aria-invalid={!!fieldError}
                          aria-describedby={
                            fieldError ? `${fieldId}-error` : undefined
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
                              <Checkbox
                                id={fieldId}
                                checked={Boolean(field.value)}
                                onCheckedChange={field.onChange}
                              />
                              <span className="text-sm">{def.label}</span>
                            </label>
                          )}
                        />
                      )}

                      {def.type === "SELECT" && (
                        <Controller
                          // eslint-disable-next-line @typescript-eslint/no-explicit-any
                          name={fieldPath as any}
                          control={control}
                          defaultValue=""
                          render={({ field }) => (
                            <Select
                              value={field.value || undefined}
                              onValueChange={field.onChange}
                            >
                              <SelectTrigger
                                id={fieldId}
                                className={
                                  fieldError ? "border-destructive" : ""
                                }
                                aria-invalid={!!fieldError}
                                aria-describedby={
                                  fieldError ? `${fieldId}-error` : undefined
                                }
                              >
                                <SelectValue placeholder="-- Choisir --" />
                              </SelectTrigger>
                              <SelectContent>
                                {def.options?.map((opt) => (
                                  <SelectItem key={opt} value={opt}>
                                    {opt}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          )}
                        />
                      )}

                      {fieldError && (
                        <p
                          id={`${fieldId}-error`}
                          role="alert"
                          className="text-sm text-destructive flex items-center"
                        >
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
              <label className="block cursor-pointer relative">
                <div
                  className={cn(
                    "flex flex-col items-center justify-center rounded-2xl border-2 border-dashed transition-all duration-200 group min-h-[280px] overflow-hidden",
                    imagePreview
                      ? "border-transparent p-0"
                      : "border-border p-8 hover:border-brand-dark hover:bg-brand-light/30",
                    isDragging &&
                      "border-brand-dark bg-brand-light/40 scale-[1.02]",
                  )}
                  onDragEnter={handleDragEnter}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                >
                  {imagePreview ? (
                    <div className="relative w-full h-full min-h-[280px]">
                      <img
                        src={imagePreview}
                        alt={nameValue ? `Aperçu de ${nameValue}` : "Aperçu de l'image"}
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
                      <Upload
                        className={cn(
                          "h-10 w-10 text-muted-foreground/50 mb-3 group-hover:text-brand-dark transition-all duration-200",
                          isDragging
                            ? "text-brand-dark scale-110"
                            : "group-hover:scale-110",
                        )}
                      />
                      <span
                        className={cn(
                          "text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors",
                          isDragging && "text-foreground",
                        )}
                      >
                        {isDragging
                          ? "Déposez l'image ici"
                          : "Télécharger une image"}
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
                {imagePreview && (
                  <Button
                    type="button"
                    variant="destructive"
                    size="icon"
                    className="absolute -top-2 -right-2 h-8 w-8 rounded-full shadow-lg z-10"
                    onClick={handleRemoveImage}
                    title="Supprimer l'image"
                  >
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </label>
              {imageFile && aiAvailable && !suggestions && !isAnalyzing && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-3"
                  onClick={() => startAnalysis(imageFile, selectedListId || undefined)}
                >
                  <Sparkles className="mr-2 h-4 w-4" />
                  Analyser avec l'IA
                </Button>
              )}
              {(isAnalyzing || suggestions) && (
                <div className="mt-4">
                  <ImageAnalysisSuggestions
                    suggestions={suggestions}
                    isAnalyzing={isAnalyzing}
                    onApply={handleApplySuggestion}
                    onApplyAll={handleApplyAllSuggestions}
                    onDismiss={resetAnalysis}
                  />
                </div>
              )}
            </div>
          </div>
        </div>

        <Separator className="my-8" />

        <div className="flex gap-4 max-w-2xl">
          <Button
            type="button"
            variant="ghost"
            className="flex-1"
            onClick={() => {
              if (!confirmDiscardChanges()) return;
              navigate(listId ? `/lists/${listId}` : "/lists");
            }}
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

      <BarcodeScannerModal
        isOpen={scannerOpen}
        onClose={() => setScannerOpen(false)}
        onScan={(code) => setValue("barcode", code, { shouldDirty: true })}
      />
    </div>
  );
}
