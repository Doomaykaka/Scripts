cd "$(dirname "${BASH_SOURCE[0]}")"
git diff --color > "$(date +"%y-%m-%d_%H_%M_%S").diff"