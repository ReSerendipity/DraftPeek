#!/usr/bin/env python3
"""DraftPeek 运行期性能基线硬门禁。

读 benchmark/performance-baseline.json 的 baselines（用例 -> 基准/容差/阈值），
将运行期实测值（macrobenchmark 产物 benchmarkData JSON，或 --measured 直传）与之比较，
任一用例超标或缺测即 exit 1（job 红），全部在容差内 exit 0（PASS）。

用法（CI 与本地同一条命令）：
  python3 benchmark/check_performance_gate.py --benchmark-data <benchmarkData.json>
  python3 benchmark/check_performance_gate.py --measured '{"startup.cold": 941.5}'
  python3 benchmark/check_performance_gate.py --list          # 列出当前门禁用例

- --benchmark-data：解析 Macrobenchmark 的 benchmarkData JSON（benchmarks[] 列表，
  name=="startup" 的 metrics.timeToInitialDisplayMs.median 映射为 startup.cold）。
- --measured：直接传入 {'<case>': <ms>}，用于本地正/负例验证与故障演练。
- 缺测（baseline 里有门禁阈值但实测缺该用例）按 FAIL 处理（fail-closed）：
  CI 上用例失败/被跳过时不允许伪装成"达标"。

门禁判据：measured_ms <= limit_ms（limit = measured 基准 * (1 + tolerance_percent/100)，
由 performance-baseline.json 预计算，避免脚本重复口径）。
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
BASELINE_PATH = os.path.join(HERE, "performance-baseline.json")

# benchmarkData JSON 中用例名 -> baseline key 的映射（只有启动了门禁的用例需要解析）
_BENCHMARK_DATA_KEY_MAP = {
    "startup": "startup.cold",
}


def load_baseline(path: str) -> dict:
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if data.get("policy") != "hard_gate":
        print(f"[perf-gate] policy={data.get('policy')!r} 非 hard_gate，跳过门禁（PASS）。")
        sys.exit(0)
    return data


def extract_measured_from_benchmark_data(path: str) -> dict:
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    benchmarks = data.get("benchmarks", [])
    measured: dict[str, float] = {}
    for entry in benchmarks:
        name = entry.get("name", "")
        key = _BENCHMARK_DATA_KEY_MAP.get(name)
        if key is None:
            continue
        metrics = entry.get("metrics", {})
        for metric_name, stat in metrics.items():
            if isinstance(stat, dict) and "median" in stat:
                measured[key] = float(stat["median"])
    return measured


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--benchmark-data", help="Macrobenchmark 产物 benchmarkData JSON 路径")
    parser.add_argument("--measured", help="直接传入实测值 JSON，如 '{\"startup.cold\": 941.5}'")
    parser.add_argument("--list", action="store_true", help="列出当前门禁用例")
    args = parser.parse_args()

    baseline = load_baseline(BASELINE_PATH)
    baselines = baseline.get("baselines", {})

    if args.list:
        print("policy =", baseline.get("policy"))
        for case, spec in sorted(baselines.items()):
            print(f"  {case}: limit={spec.get('limit_ms')}ms (基准 {spec.get('measured_ms')}ms "
                  f"+{spec.get('tolerance_percent')}%) metric={spec.get('metric')}")
        return 0

    if args.measured:
        measured = json.loads(args.measured)
    elif args.benchmark_data:
        measured = extract_measured_from_benchmark_data(args.benchmark_data)
    else:
        parser.error("必须提供 --benchmark-data 或 --measured 之一")

    failures: list[str] = []
    checked = 0
    for case, spec in sorted(baselines.items()):
        limit = spec.get("limit_ms")
        if limit is None:
            continue
        checked += 1
        value = measured.get(case)
        if value is None:
            failures.append(f"  {case}: 缺测（baseline 有门禁阈值但实测无该用例，fail-closed）")
            continue
        verdict = "PASS" if value <= limit else "FAIL"
        print(f"[perf-gate] {case}: measured={value:.1f}ms limit={limit:.1f}ms -> {verdict}")
        if verdict == "FAIL":
            failures.append(f"  {case}: {value:.1f}ms 超阈值 {limit:.1f}ms（超标 {value / limit * 100 - 100:.1f}%）")

    if failures:
        print(f"[perf-gate] {len(failures)}/{checked} 项未通过：")
        for line in failures:
            print(line)
        print("[perf-gate] RESULT: FAIL（macrobenchmark 超标即红）")
        return 1

    print(f"[perf-gate] {checked} 项全部在容差内。RESULT: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
