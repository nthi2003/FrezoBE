#!/usr/bin/env python3
"""Add @CheckPermission annotations to REST controller endpoints."""
import re
import glob
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

EXCLUDE_CONTROLLERS = {
    "AuthController",
    "PublicController",
    "PublicLeadController",
    "PublicRedirectController",
    "EventPortalController",
    "InternalGatewayController",
    "TestWebSocketController",
    "ContractSignController",
}

ACTION_POST_EXCEPTIONS = (
    "approve", "publish", "activate", "deactivate", "reject", "submit",
    "revoke", "heartbeat", "cancel", "complete", "close", "reopen",
    "sign", "verify", "confirm", "assign", "unassign", "transfer",
    "lock", "unlock", "archive", "restore", "send", "resend", "retry",
    "import", "export", "sync", "refresh", "reset", "withdraw",
)


def infer_action(http_method: str, sub_path: str, method_name: str) -> str:
    combined = f"{sub_path} {method_name}".lower()
    if http_method == "GET":
        return "VIEW"
    if http_method == "DELETE":
        return "DELETE"
    if http_method in ("PUT", "PATCH"):
        return "UPDATE"
    if http_method == "POST":
        if "pageview" in combined:
            return "CREATE"
        if any(k in combined for k in ACTION_POST_EXCEPTIONS):
            return "UPDATE"
        return "CREATE"
    return "VIEW"


def should_skip_endpoint(class_name: str, sub_path: str, method_name: str, block: str) -> bool:
    if class_name == "MenuController":
        sp = sub_path.lower()
        if sp.startswith("/user/") or method_name == "getMenusForUser":
            return True
    if class_name == "PermissionController" and sub_path.rstrip("/") == "/combobox":
        return True
    if class_name == "SystemController":
        sp = sub_path.rstrip("/").lower()
        if sp in ("/health", "/info", "health", "info"):
            return True
    return False


def get_special_api(class_name: str, base_path: str) -> str | None:
    if class_name == "SessionController":
        return "/auth/session"
    if class_name == "UserActivityController":
        return "/auth/statistic"
    if class_name == "UsageAnalyticsController":
        return "/qtht/usage"
    return base_path


def process_file(path: Path) -> bool:
    content = path.read_text(encoding="utf-8")
    original = content

    cls_match = re.search(r"class\s+(\w+Controller)\b", content)
    if not cls_match:
        return False
    class_name = cls_match.group(1)
    if class_name in EXCLUDE_CONTROLLERS:
        return False

    base_match = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', content)
    if not base_match:
        return False
    base_path = base_match.group(1)
    api_path = get_special_api(class_name, base_path)

    if "import com.frezo.common.security.CheckPermission;" not in content:
        if "import com.frezo.common.response" in content:
            content = re.sub(
                r"(import com\.frezo\.common\.response\.\w+;)",
                r"\1\nimport com.frezo.common.security.CheckPermission;",
                content,
                count=1,
            )
        elif re.search(r"^import ", content, re.M):
            content = re.sub(
                r"(^import .+?;\n)",
                r"\1import com.frezo.common.security.CheckPermission;\n",
                content,
                count=1,
                flags=re.M,
            )
        else:
            content = content.replace(
                "package ",
                "import com.frezo.common.security.CheckPermission;\n\npackage ",
                1,
            )

    lines = content.split("\n")
    new_lines = []
    i = 0
    changed = False
    while i < len(lines):
        line = lines[i]
        mapping_match = re.match(
            r"(\s*)@(Get|Post|Put|Patch|Delete)Mapping(?:\((.*)\))?\s*$",
            line,
        )
        if mapping_match:
            indent = mapping_match.group(1)
            http_method = mapping_match.group(2).upper()
            args = mapping_match.group(3) or ""
            sub_path = ""
            sm = re.search(r'["\']([^"\']*)["\']', args)
            if sm:
                sub_path = sm.group(1)
                if not sub_path.startswith("/"):
                    sub_path = "/" + sub_path

            block = [line]
            j = i + 1
            has_cp = False
            method_name = ""
            while j < len(lines) and j < i + 12:
                bl = lines[j]
                if re.match(r"\s*@(Get|Post|Put|Patch|Delete)Mapping", bl):
                    break
                if "@CheckPermission" in bl and not bl.strip().startswith("//"):
                    has_cp = True
                if bl.strip().startswith("//") and "@CheckPermission" in bl:
                    has_cp = "commented"
                fn = re.search(r"public\s+(?:[\w<>,\s\[\]?]+\s+)+(\w+)\s*\(", bl)
                if fn:
                    method_name = fn.group(1)
                    break
                block.append(bl)
                j += 1

            skip = should_skip_endpoint(class_name, sub_path, method_name, "\n".join(block))
            if not skip and not has_cp:
                action = infer_action(http_method, sub_path, method_name)
                if class_name == "UsageAnalyticsController":
                    if "pageview" in sub_path.lower() or "pageview" in method_name.lower():
                        action = "CREATE"
                    elif http_method == "GET":
                        action = "VIEW"
                if class_name == "SessionController":
                    if http_method == "GET":
                        action = "VIEW"
                    else:
                        action = "UPDATE"
                cp_line = f'{indent}@CheckPermission(api = "{api_path}", action = "{action}")'
                new_lines.append(line)
                new_lines.append(cp_line)
                changed = True
                i += 1
                continue
            elif has_cp == "commented":
                action = infer_action(http_method, sub_path, method_name)
                if class_name == "UsageAnalyticsController" and "pageview" in sub_path.lower():
                    action = "CREATE"
                if class_name == "SessionController" and http_method == "GET":
                    action = "VIEW"
                elif class_name == "SessionController":
                    action = "UPDATE"
                cp_line = f'{indent}@CheckPermission(api = "{api_path}", action = "{action}")'
                new_lines.append(line)
                k = i + 1
                while k < len(lines):
                    if lines[k].strip().startswith("//") and "@CheckPermission" in lines[k]:
                        k += 1
                        continue
                    break
                new_lines.append(cp_line)
                changed = True
                i += 1
                continue

        new_lines.append(line)
        i += 1

    if changed:
        new_content = "\n".join(new_lines)
        if new_content != original:
            path.write_text(new_content, encoding="utf-8")
            return True
    return False


def main():
    pattern = str(ROOT / "**" / "src" / "main" / "java" / "**" / "*Controller.java")
    files = sorted(Path(p) for p in glob.glob(pattern, recursive=True))
    modified = []
    for f in files:
        if process_file(f):
            modified.append(str(f.relative_to(ROOT)))
    print(f"Modified {len(modified)} controllers:")
    for m in modified:
        print(f"  {m}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
