import { BlurFade } from "@/components/effects/blur-fade";
import { SpotlightCard } from "@/components/effects/spotlight-card";
import {
  StaggeredList,
  StaggeredItem,
} from "@/components/effects/staggered-list";
import { Badge } from "@/components/ui/badge";
import { FEATURES, MOCK_LIST_CARDS } from "./mockData";

export function FeaturesSection() {
  return (
    <section id="features" aria-labelledby="features-heading" className="py-20 md:py-32">
      <div className="max-w-6xl mx-auto px-6">
        <BlurFade delay={0.1} inView>
          <div className="text-center mb-12 md:mb-16">
            <h2 id="features-heading" className="font-display text-3xl md:text-4xl font-semibold tracking-tight mb-4">
              Tout ce dont vous avez besoin
            </h2>
            <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
              Des outils puissants pour gerer votre inventaire efficacement,
              quelle que soit la taille de votre activite.
            </p>
          </div>
        </BlurFade>

        <StaggeredList
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6"
          staggerDelay={0.06}
        >
          {FEATURES.map((feature, idx) => (
            <StaggeredItem key={idx}>
              <SpotlightCard className="rounded-2xl border bg-card p-6 shadow-card transition-all duration-300 hover:shadow-elevated h-full">
                <div className="w-11 h-11 rounded-xl bg-brand-light flex items-center justify-center mb-4">
                  <feature.icon className="h-5 w-5 text-brand-dark" aria-hidden="true" />
                </div>
                <h3 className="font-display text-lg font-semibold tracking-tight mb-2">
                  {feature.title}
                </h3>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  {feature.description}
                </p>
              </SpotlightCard>
            </StaggeredItem>
          ))}
        </StaggeredList>
      </div>
    </section>
  );
}

export function ListsPreviewSection() {
  return (
    <section aria-labelledby="lists-heading" className="py-20 md:py-32">
      <div className="max-w-6xl mx-auto px-6">
        <BlurFade delay={0.1} inView>
          <div className="text-center mb-12 md:mb-16">
            <h2 id="lists-heading" className="font-display text-3xl md:text-4xl font-semibold tracking-tight mb-4">
              Des listes puissantes et flexibles
            </h2>
            <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
              Creez des listes personnalisees avec des champs sur mesure pour
              organiser votre inventaire comme vous le souhaitez.
            </p>
          </div>
        </BlurFade>

        <StaggeredList
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6"
          staggerDelay={0.06}
        >
          {MOCK_LIST_CARDS.map((list, idx) => (
            <StaggeredItem key={idx}>
              <SpotlightCard className="group rounded-2xl border bg-card shadow-card transition-all duration-300 hover:shadow-elevated overflow-hidden">
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-brand-light flex items-center justify-center">
                      <span className="font-display text-xl font-bold text-foreground">
                        {list.name[0]?.toUpperCase()}
                      </span>
                    </div>
                    <Badge variant="secondary">{list.category}</Badge>
                  </div>
                  <h3 className="font-display text-lg font-semibold tracking-tight mb-1 group-hover:text-brand-dark transition-colors">
                    {list.name}
                  </h3>
                  <p className="text-muted-foreground text-sm line-clamp-2 mb-3">
                    {list.description}
                  </p>
                  <p className="text-muted-foreground text-sm">
                    <span className="font-semibold text-foreground">
                      {list.itemCount}
                    </span>{" "}
                    articles
                  </p>
                  {list.customFields.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mt-3">
                      {list.customFields.map((field) => (
                        <span
                          key={field}
                          className="inline-flex items-center px-2 py-0.5 rounded-md bg-secondary text-xs font-medium text-muted-foreground"
                        >
                          {field}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </SpotlightCard>
            </StaggeredItem>
          ))}
        </StaggeredList>
      </div>
    </section>
  );
}
