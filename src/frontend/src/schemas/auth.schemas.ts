import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().email('Email invalide'),
  password: z.string().min(1, 'Le mot de passe est requis'),
});

export const createUserSchema = z.object({
  email: z.string().email('Email invalide'),
  password: z.string()
    .min(12, 'Le mot de passe doit contenir au moins 12 caractères')
    .regex(/[a-z]/, 'Doit contenir au moins une lettre minuscule')
    .regex(/[A-Z]/, 'Doit contenir au moins une lettre majuscule')
    .regex(/[0-9]/, 'Doit contenir au moins un chiffre')
    .regex(/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~`]/, 'Doit contenir au moins un caractère spécial (!@#$%^&*...)'),
  role: z.enum(['USER', 'ADMIN']),
});

export const signupSchema = z.object({
  email: z.string().email('Email invalide'),
  password: z.string()
    .min(12, 'Le mot de passe doit contenir au moins 12 caractères')
    .regex(/[a-z]/, 'Doit contenir au moins une lettre minuscule')
    .regex(/[A-Z]/, 'Doit contenir au moins une lettre majuscule')
    .regex(/[0-9]/, 'Doit contenir au moins un chiffre')
    .regex(/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~`]/, 'Doit contenir au moins un caractère spécial (!@#$%^&*...)'),
  confirmPassword: z.string().min(1, 'La confirmation du mot de passe est requise'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Les mots de passe ne correspondent pas',
  path: ['confirmPassword'],
});

export const forgotPasswordSchema = z.object({
  email: z.string().email('Email invalide'),
});

export const resetPasswordSchema = z.object({
  password: z.string()
    .min(12, 'Le mot de passe doit contenir au moins 12 caractères')
    .regex(/[a-z]/, 'Doit contenir au moins une lettre minuscule')
    .regex(/[A-Z]/, 'Doit contenir au moins une lettre majuscule')
    .regex(/[0-9]/, 'Doit contenir au moins un chiffre')
    .regex(/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~`]/, 'Doit contenir au moins un caractère spécial (!@#$%^&*...)'),
  confirmPassword: z.string().min(1, 'La confirmation du mot de passe est requise'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Les mots de passe ne correspondent pas',
  path: ['confirmPassword'],
});

export type LoginFormData = z.infer<typeof loginSchema>;
export type CreateUserFormData = z.infer<typeof createUserSchema>;
export type SignupFormData = z.infer<typeof signupSchema>;
export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;
