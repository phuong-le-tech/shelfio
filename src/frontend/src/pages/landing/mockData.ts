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
  toVerifyCount: 23,
  needsAttentionCount: 5,
};

export const MOCK_LISTS_OVERVIEW = [
  { listName: "Électronique", itemsCount: 234, totalQuantity: 4_120 },
  { listName: "Fournitures Bureau", itemsCount: 189, totalQuantity: 3_450 },
  { listName: "Mobilier", itemsCount: 76, totalQuantity: 890 },
];

export const MOCK_RECENT_ITEMS = [
  {
    name: "MacBook Pro 14\"",
    listName: "Électronique",
    quantity: 12,
    status: "AVAILABLE" as const,
    lastUpdated: "2026-03-04T10:30:00",
  },
  {
    name: "Chaise ergonomique",
    listName: "Mobilier",
    quantity: 3,
    status: "TO_VERIFY" as const,
    lastUpdated: "2026-03-03T14:15:00",
  },
  {
    name: "Cartouches d'encre",
    listName: "Fournitures Bureau",
    quantity: 0,
    status: "DAMAGED" as const,
    lastUpdated: "2026-03-02T09:45:00",
  },
];

export const MOCK_LIST_CARDS = [
  {
    name: "Électronique",
    description: "Appareils et accessoires électroniques",
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
    description: "Meubles et équipements de bureau",
    category: "Aménagement",
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
      "Vue d'ensemble de votre inventaire avec statistiques en temps réel",
  },
  {
    icon: List,
    title: "Listes organisées",
    description:
      "Organisez vos articles par listes avec catégories personnalisées",
  },
  {
    icon: Settings2,
    title: "Champs personnalisés",
    description: "Ajoutez des champs Texte, Nombre, Date ou Oui/Non à vos listes",
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
      "Trouvez rapidement n'importe quel article avec la recherche avancée",
  },
  {
    icon: Users,
    title: "Multi-utilisateurs",
    description:
      "Comptes sécurisés avec rôles Utilisateur et Administrateur",
  },
];
