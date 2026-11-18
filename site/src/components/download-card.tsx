import { ArrowDownToLine, ExternalLink } from "lucide-react";
import { formatSize, releaseLink, type Release } from "../lib/releases";

export function DownloadCard({ release }: { release: Release | null }) {
  if (!release?.jar)
    return (
      <div className="download-card glass">
        <p>
          {release
            ? "The latest release has no single mod JAR. Its asset list has the files and notes."
            : "This site build has no release snapshot. GitHub has the latest files and notes."}
        </p>
        <a className="button primary" href={releaseLink(release)}>
          Open the latest release
          <ExternalLink size={16} aria-hidden="true" />
        </a>
      </div>
    );
  const { jar } = release;
  return (
    <div className="download-card glass">
      <dl className="release-facts">
        <div>
          <dt>Version</dt>
          <dd className="mono">{release.tag}</dd>
        </div>
        {release.minecraft && (
          <div>
            <dt>Minecraft</dt>
            <dd className="mono">{release.minecraft}</dd>
          </div>
        )}
        <div>
          <dt>Loader</dt>
          <dd>Fabric</dd>
        </div>
        <div>
          <dt>Size</dt>
          <dd className="mono">{formatSize(jar.size)}</dd>
        </div>
      </dl>
      <a className="download-chip" href={jar.url} aria-label={`Download ${jar.name}`}>
        <span className="chip-icon">
          <ArrowDownToLine size={20} aria-hidden="true" />
        </span>
        <span className="chip-text">
          <strong>Download the mod</strong>
          <span className="mono">{jar.name}</span>
        </span>
      </a>
      <p className="download-meta">
        <a href={release.url}>Release notes</a>
        <span>Needs Fabric API</span>
      </p>
    </div>
  );
}
