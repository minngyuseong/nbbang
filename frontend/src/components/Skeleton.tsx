import type { HTMLAttributes } from "react";
import "./skeleton.css";

export function Skeleton({ className = "", ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={`skeleton ${className}`} {...props} />;
}

export function PageSkeleton() {
  return (
    <main className="app-shell skeleton-page" aria-label="불러오는 중">
      <div className="skeleton-delay">
        <Skeleton className="skeleton-header" />
        <Skeleton className="skeleton-title" />
        <Skeleton className="skeleton-card" />
        <Skeleton className="skeleton-card" />
        <Skeleton className="skeleton-row" />
        <Skeleton className="skeleton-row" />
      </div>
    </main>
  );
}
