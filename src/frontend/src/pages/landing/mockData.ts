import {
  LayoutDashboard,
  List,
  Settings2,
  BarChart3,
  Search,
  Users,
  type LucideIcon,
} from "lucide-react";

export const MOCK_STATS = {
  totalItems: 847,
  totalQuantity: 12_340,
  lowStockCount: 23,
  outOfStockCount: 5,
};

export const MOCK_LISTS_OVERVIEW = [
  { listName: "Electronique", itemsCount: 234, totalQuantity: 4_120 },
  { listName: "Fournitures Bureau", itemsCount: 189, totalQuantity: 3_450 },
  { listName: "Mobilier", itemsCount: 76, totalQuantity: 890 },
];

export const MOCK_RECENT_ITEMS = [
  {
    name: "MacBook Pro 14\"",
    listName: "Electronique",
    quantity: 12,
    status: "IN_STOCK" as const,
    lastUpdated: "2026-03-04T10:30:00",
  },
  {
    name: "Chaise ergonomique",
    listName: "Mobilier",
    quantity: 3,
    status: "LOW_STOCK" as const,
    lastUpdated: "2026-03-03T14:15:00",
  },
  {
    name: "Cartouches d'encre",
    listName: "Fournitures Bureau",
    quantity: 0,
    status: "OUT_OF_STOCK" as const,
    lastUpdated: "2026-03-02T09:45:00",
  },
];

export const MOCK_LIST_CARDS = [
  {
    name: "Electronique",
    description: "Appareils et accessoires electroniques",
    category: "Tech",
    itemCount: 234,
    customFields: ["Texte", "Nombre"],
  },
  {
    name: "Fournitures Bureau",
    description: "Articles de bureau et consommables",
    category: "Bureau",
    itemCount: 189,
    customFields: ["Date", "Oui/Non", "Nombre"],
  },
  {
    name: "Mobilier",
    description: "Meubles et equipements de bureau",
    category: "Amenagement",
    itemCount: 76,
    customFields: [],
  },
];

interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
}

export const FEATURES: Feature[] = [
  {
    icon: LayoutDashboard,
    title: "Tableau de bord",
    description:
      "Vue d'ensemble de votre inventaire avec statistiques en temps reel",
  },
  {
    icon: List,
    title: "Listes organisees",
    description:
      "Organisez vos articles par listes avec categories personnalisees",
  },
  {
    icon: Settings2,
    title: "Champs personnalises",
    description: "Ajoutez des champs Texte, Nombre, Date ou Oui/Non a vos listes",
  },
  {
    icon: BarChart3,
    title: "Suivi du stock",
    description:
      "Visualisez les niveaux de stock et identifiez les ruptures",
  },
  {
    icon: Search,
    title: "Recherche et filtres",
    description:
      "Trouvez rapidement n'importe quel article avec la recherche avancee",
  },
  {
    icon: Users,
    title: "Multi-utilisateurs",
    description:
      "Comptes securises avec roles Utilisateur et Administrateur",
  },
];
