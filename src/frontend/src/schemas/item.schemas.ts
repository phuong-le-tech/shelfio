import { z } from 'zod';
import type { CustomFieldDefinition } from '../types/item';

// ---------------------------------------------------------------------------
// ListForm schema
// ---------------------------------------------------------------------------

const customFieldDefinitionSchema = z.object({
  name: z.string().default(''),
  label: z.string().min(1, 'Le libellé est requis'),
  type: z.enum(['TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'SELECT']),
  required: z.boolean(),
  displayOrder: z.number().int().min(0),
  options: z.array(z.string()).optional(),
});

export const listFormSchema = z.object({
  name: z
    .string()
    .min(1, 'Le nom est requis')
    .max(100, 'Le nom ne doit pas dépasser 100 caractères'),
  description: z
    .string()
    .max(500, 'La description est trop longue')
    .optional()
    .or(z.literal('')),
  category: z.string().max(100).optional().or(z.literal('')),
  customFieldDefinitions: z.array(customFieldDefinitionSchema).optional(),
});

// ---------------------------------------------------------------------------
// ItemForm schema — factory that captures required custom fields
// ---------------------------------------------------------------------------

export function createItemSchema(fieldDefs: CustomFieldDefinition[] = []) {
  return z.object({
    name: z.string().min(1, 'Le nom est requis').max(200),
    itemListId: z.string().min(1, 'La liste est requise'),
    status: z.string(),
    stock: z.number().int().min(0, 'Le stock ne peut pas être négatif'),
    customFieldValues: z
      .record(z.string(), z.unknown())
      .optional()
      .superRefine((vals, ctx) => {
        for (const def of fieldDefs) {
          if (!def.required) continue;
          const v = vals?.[def.name];
          const isEmpty =
            v === undefined ||
            v === null ||
            v === '' ||
            (typeof v === 'number' && isNaN(v));
          if (isEmpty) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: `${def.label} est requis`,
              path: [def.name],
            });
          }
        }
      }),
  });
}
