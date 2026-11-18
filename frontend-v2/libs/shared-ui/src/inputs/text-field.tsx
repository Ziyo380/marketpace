import { useId, type InputHTMLAttributes } from 'react';

type TextFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> & {
  id?: string;
  label: string;
  help?: string;
  error?: string;
};

export function TextField({ id, label, help, error, className = '', ...props }: TextFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const description = error ? `${inputId}-error` : help ? `${inputId}-help` : undefined;

  return (
    <div className="ds-field">
      <label className="ds-label" htmlFor={inputId}>{label}</label>
      <input
        {...props}
        id={inputId}
        className={`ds-input ${className}`.trim()}
        aria-invalid={error ? true : undefined}
        aria-describedby={description}
      />
      {error ? <span className="ds-error" id={description}>{error}</span> : help ? <span className="ds-help" id={description}>{help}</span> : null}
    </div>
  );
}
