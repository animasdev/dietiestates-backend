import subprocess
from collections import Counter, defaultdict

def git_log(args):
    result = subprocess.run(
        ["git"] + args,
        capture_output=True,
        text=True,
        check=True
    )
    return result.stdout.strip().splitlines()

# 1) Commits per month
dates = git_log(["log", "--date=short", "--pretty=%ad"])
months = [d[:7] for d in dates]  # YYYY-MM
commits_per_month = Counter(months)

# 2) Commits per weekday
weekdays = git_log(["log", "--date=format:%a", "--pretty=%ad"])
commits_per_weekday = Counter(weekdays)

# 3) Files touched per commit
# Use --shortstat to get "X files changed ..."
shortstat_lines = git_log(["log", "--shortstat", "--pretty=%h"])
files_per_commit = []

for line in shortstat_lines:
    line = line.strip()
    if "file changed" in line or "files changed" in line:
        # line looks like: "3 files changed, 10 insertions(+), 2 deletions(-)"
        count = int(line.split()[0])
        files_per_commit.append(count)

total_commits = len(files_per_commit)
total_files = sum(files_per_commit)
avg_files = total_files / total_commits if total_commits else 0
max_files = max(files_per_commit) if files_per_commit else 0
dist_files = Counter(files_per_commit)

print("=== COMMITS PER MONTH ===")
for month, count in sorted(commits_per_month.items()):
    print(f"{month}: {count}")

print("\n=== COMMITS PER WEEKDAY ===")
for day in ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]:
    if day in commits_per_weekday:
        print(f"{day}: {commits_per_weekday[day]}")

print("\n=== FILES TOUCHED PER COMMIT ===")
print(f"Commits with changes: {total_commits}")
print(f"Total files changed: {total_files}")
print(f"Average files per commit: {avg_files:.2f}")
print(f"Max files in a commit: {max_files}")

print("\nDistribution (commits touching N files):")
for n in sorted(dist_files):
    print(f"{n} files: {dist_files[n]} commits")
