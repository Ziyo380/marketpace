import type { ButtonHTMLAttributes } from 'react';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost';
};

export function Button({ variant = 'primary', className = '', type = 'button', ...props }: ButtonProps) {
  return <button type={type} className={`ds-button ds-button--${variant} ${className}`.trim()} {...props} />;
}
