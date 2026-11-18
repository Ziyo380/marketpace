# Dark Flame design system

Both apps import the same CSS files from `src/styles` in this order: Tailwind v4, `theme.css`, `tokens.css`, `global-base.css`, `utilities.css`. The app stylesheet registers its own source and `libs/shared-ui/src` with Tailwind.

- `theme.css` defines Tailwind colors, fonts, breakpoints and radii through `@theme static`.
- `tokens.css` holds semantic CSS custom properties for borders, glass, glow, transitions, statuses and layout.
- `global-base.css` supplies the shared dark base, focus ring and reduced-motion handling.
- `utilities.css` supplies `ds-container`, `glass-panel`, glow helpers and component classes for buttons, fields, cards, dialogs and badges.

Spacing follows Tailwind's 4px base scale. Side gutters are 16px, 24px and 48px at mobile, tablet and desktop widths. The content cap is 1440px.

Fonts are bundled locally from the OFL-licensed `@fontsource/bebas-neue` and `@fontsource-variable/manrope` packages; neither app needs a runtime Google Fonts request.

Shared React components are in `libs/shared-ui`. `Modal` uses the native modal dialog for keyboard focus containment and Escape handling. Use text labels alongside badge colors, and pair input errors with a visible message.
