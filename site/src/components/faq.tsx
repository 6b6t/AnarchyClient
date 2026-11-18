import { Plus } from "lucide-react";
import { useState } from "react";

export function Faq({ items }: { items: { question: string; answer: string }[] }) {
  const [open, setOpen] = useState<number | null>(0);
  return (
    <div className="faq">
      {items.map(({ question, answer }, index) => {
        const expanded = open === index;
        return (
          <div className="faq-item" key={question} data-open={expanded}>
            <h3>
              <button
                type="button"
                aria-expanded={expanded}
                aria-controls={`faq-${index}`}
                id={`faq-q-${index}`}
                onClick={() => setOpen(expanded ? null : index)}
              >
                {question}
                <Plus className="faq-icon" size={20} aria-hidden="true" />
              </button>
            </h3>
            <div
              className="faq-answer"
              id={`faq-${index}`}
              role="region"
              aria-labelledby={`faq-q-${index}`}
              inert={!expanded}
            >
              <div>
                <p>{answer}</p>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
