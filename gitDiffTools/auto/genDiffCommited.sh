cd "$(dirname "${BASH_SOURCE[0]}")"
git diff --color HEAD^ HEAD > "$(date +"%y-%m-%d_%H_%M_%S").diff"