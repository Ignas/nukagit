#!/bin/bash

create_repo() {
    repo_name=$1
    num_commits=$2
    num_branches=$3

    git init "$repo_name"
    cd "$repo_name" || exit
    echo "Initial commit" > README.md
    git add README.md
    git commit -m "Initial commit"

    for i in $(seq 1 "$num_commits"); do
        echo "Commit $i" >> README.md
        git add README.md
        git commit -m "Commit $i"
    done

    for i in $(seq 1 "$num_branches"); do
        branch_name="branch$i"
        git checkout -b "$branch_name"
        for j in $(seq 1 10); do
            echo "Commit $i.$j" >> README.md
            git add README.md
            git commit -m "Commit $i.$j"
        done
    done

    git checkout main
    git remote add origin "ssh://git@localhost:2222/$repo_name"
    echo "Repository $repo_name created."
}

# Usage: create_repo <repository_name> <num_commits> <num_branches>
create_repo "testrepo" 1000 100
