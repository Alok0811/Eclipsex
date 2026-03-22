#!/usr/bin/env python3
"""
Eclipse AI Planning Agent
Reads the repository, analyzes specs, and generates actionable development tasks.
Run: python3 scripts/planner.py [--full | --quick | --security | --build]
"""

import os
import sys
import json
import hashlib
from pathlib import Path
from datetime import datetime

REPO_ROOT = Path(__file__).resolve().parent.parent
APP_SRC = REPO_ROOT / "app" / "src" / "main" / "java" / "com" / "eclipse" / "browser"
SPEC_FILES = [
    "Eclipse_Dawn_0.1_Specification.md",
    "Eclipse-UX-Motion-Specification.md",
    "Eclipse-Native-Conversion-Prompt.md",
]
PLAN_OUTPUT = REPO_ROOT / "scripts" / ".plan_cache.json"


def find_kotlin_files():
    """Return all .kt source files."""
    kt_files = []
    for path in APP_SRC.rglob("*.kt"):
        rel = path.relative_to(REPO_ROOT)
        size = path.stat().st_size
        kt_files.append({"path": str(rel), "size": size, "name": path.stem})
    return sorted(kt_files, key=lambda f: f["path"])


def find_xml_resources():
    """Return all XML resource files."""
    res_dir = REPO_ROOT / "app" / "src" / "main" / "res"
    xml_files = []
    if res_dir.exists():
        for path in res_dir.rglob("*.xml"):
            rel = path.relative_to(REPO_ROOT)
            xml_files.append(str(rel))
    return sorted(xml_files)


def read_spec_summaries():
    """Read specification files and extract key sections."""
    summaries = {}
    for spec in SPEC_FILES:
        spec_path = REPO_ROOT / spec
        if spec_path.exists():
            content = spec_path.read_text(errors="replace")
            # Extract headings as task indicators
            headings = []
            for line in content.split("\n"):
                stripped = line.strip()
                if stripped.startswith("#"):
                    headings.append(stripped.lstrip("#").strip())
            summaries[spec] = {
                "headings": headings[:30],
                "size": len(content),
                "lines": content.count("\n") + 1,
            }
    return summaries


def check_build_health():
    """Check for common build issues."""
    issues = []

    gradle_file = REPO_ROOT / "app" / "build.gradle"
    if gradle_file.exists():
        content = gradle_file.read_text(errors="replace")
        if "compileSdk 34" not in content and "compileSdk = 34" not in content:
            issues.append("compileSdk may need updating")
        if "minSdk" not in content:
            issues.append("minSdk not found in build.gradle")
    else:
        issues.append("app/build.gradle not found")

    manifest = REPO_ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
    if not manifest.exists():
        issues.append("AndroidManifest.xml not found")

    gradlew = REPO_ROOT / "gradlew"
    if gradlew.exists() and not os.access(gradlew, os.X_OK):
        issues.append("gradlew is not executable")

    local_props = REPO_ROOT / "local.properties"
    if local_props.exists():
        content = local_props.read_text(errors="replace")
        if "sdk.dir" not in content:
            issues.append("local.properties missing sdk.dir")

    return issues


def check_security():
    """Basic security checks on the codebase."""
    warnings = []
    for kt_file in APP_SRC.rglob("*.kt"):
        content = kt_file.read_text(errors="replace")
        rel = kt_file.relative_to(REPO_ROOT)

        if "setJavaScriptEnabled(true)" in content:
            warnings.append(f"{rel}: JavaScript enabled in WebView — ensure input is sanitized")
        if "addJavascriptInterface" in content:
            warnings.append(f"{rel}: JavascriptInterface exposed — review for injection risk")
        if "MODE_WORLD_READABLE" in content or "MODE_WORLD_WRITEABLE" in content:
            warnings.append(f"{rel}: World-readable/writable file mode detected")
        if "Log.d(" in content or "Log.v(" in content:
            lines_with_log = [
                i + 1 for i, line in enumerate(content.split("\n"))
                if "Log.d(" in line or "Log.v(" in line
            ]
            if lines_with_log:
                warnings.append(f"{rel}: Debug logging at lines {lines_with_log[:5]} — strip before release")
        if "http://" in content and "https://" not in content:
            warnings.append(f"{rel}: Insecure HTTP URL found — use HTTPS")

    keystore = REPO_ROOT / "eclipse-release.keystore"
    if keystore.exists():
        warnings.append("eclipse-release.keystore is in repo — should be in .gitignore / secrets")

    return warnings


def generate_tasks(mode="full"):
    """Generate the task list based on analysis."""
    print(f"\n{'='*60}")
    print(f"  Eclipse AI Planning Agent — {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    print(f"  Mode: {mode}")
    print(f"{'='*60}\n")

    # Gather data
    kt_files = find_kotlin_files()
    xml_files = find_xml_resources()
    specs = read_spec_summaries()
    build_issues = check_build_health()
    security_warnings = check_security()

    print(f"[Scan] {len(kt_files)} Kotlin files, {len(xml_files)} XML resources")
    print(f"[Scan] {len(specs)} spec files, {len(build_issues)} build issues, {len(security_warnings)} security warnings\n")

    tasks = []
    task_id = 0

    # --- Build tasks ---
    if mode in ("full", "build"):
        if build_issues:
            task_id += 1
            tasks.append({
                "id": task_id,
                "category": "BUILD",
                "priority": "HIGH",
                "title": "Fix build issues",
                "details": build_issues,
            })

        task_id += 1
        tasks.append({
            "id": task_id,
            "category": "BUILD",
            "priority": "MEDIUM",
            "title": "Verify Gradle build passes",
            "details": ["Run: ./gradlew assembleDebug --no-daemon"],
        })

    # --- Security tasks ---
    if mode in ("full", "security"):
        if security_warnings:
            for w in security_warnings:
                task_id += 1
                tasks.append({
                    "id": task_id,
                    "category": "SECURITY",
                    "priority": "HIGH",
                    "title": "Security review needed",
                    "details": [w],
                })

    # --- Spec-driven tasks ---
    if mode in ("full",):
        for spec_name, info in specs.items():
            for heading in info["headings"]:
                lower = heading.lower()
                # Filter to actionable headings
                if any(kw in lower for kw in [
                    "remove", "fix", "add", "implement", "redesign",
                    "create", "update", "integrate", "build", "polish",
                ]):
                    task_id += 1
                    tasks.append({
                        "id": task_id,
                        "category": "FEATURE",
                        "priority": "MEDIUM",
                        "title": heading,
                        "details": [f"Source: {spec_name}"],
                    })

    # --- Code quality tasks ---
    if mode in ("full", "quick"):
        large_files = [f for f in kt_files if f["size"] > 15000]
        if large_files:
            task_id += 1
            tasks.append({
                "id": task_id,
                "category": "QUALITY",
                "priority": "LOW",
                "title": "Consider splitting large files",
                "details": [f"{f['path']} ({f['size']//1024}KB)" for f in large_files],
            })

    # --- Print results ---
    if not tasks:
        print("  No tasks generated. Project looks clean!\n")
        return tasks

    for t in tasks:
        icon = {"HIGH": "🔴", "MEDIUM": "🟡", "LOW": "🟢"}.get(t["priority"], "⚪")
        print(f"  {icon} [{t['category']}] #{t['id']}: {t['title']}")
        for d in t["details"]:
            print(f"      → {d}")
        print()

    # Save plan cache
    plan_data = {
        "generated": datetime.now().isoformat(),
        "mode": mode,
        "stats": {
            "kotlin_files": len(kt_files),
            "xml_resources": len(xml_files),
            "build_issues": len(build_issues),
            "security_warnings": len(security_warnings),
        },
        "tasks": tasks,
    }
    PLAN_OUTPUT.write_text(json.dumps(plan_data, indent=2))
    print(f"  [{len(tasks)} tasks generated → scripts/.plan_cache.json]\n")

    return tasks


def main():
    mode = "full"
    if len(sys.argv) > 1:
        arg = sys.argv[1].lstrip("-")
        if arg in ("full", "quick", "security", "build"):
            mode = arg
        else:
            print(f"Usage: {sys.argv[0]} [--full | --quick | --security | --build]")
            print("  --full     All checks (default)")
            print("  --quick    Code quality only")
            print("  --security Security scan only")
            print("  --build    Build health only")
            sys.exit(1)

    generate_tasks(mode)


if __name__ == "__main__":
    main()
