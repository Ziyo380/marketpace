import type { HTMLAttributes } from 'react';

type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: 'success' | 'warning' | 'error' | 'neutral' | 'active';
};

export function Badge({ tone = 'neutral', className = '', ...props }: BadgeProps) {
  return <span className={`ds-badge ds-badge--${tone} ${className}`.trim()} {...props} />;
}
