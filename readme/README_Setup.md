<a id="top"></a>

# Setup README

## Overview

This document explains two separate things:

1. how we work as a team on GitHub and Git
2. how to set up and run the project locally

The base repository is:

- [Neueda-Learning/11_105_CTRL-ALT-DEFEAT: Payment Processing](https://github.com/Neueda-Learning/11_105_CTRL-ALT-DEFEAT)

## Quick Navigation

- [How We Work](#how-we-work)
- [Local Project Setup](#local-project-setup)
- [Initial Setup](#initial-setup)
- [Clone Your Fork](#clone-your-fork)
- [Keep Base Repo as Upstream](#keep-base-repo-as-upstream)
- [Create a Branch](#create-a-branch)
- [Make Changes and Commit](#make-changes-and-commit)
- [Push to Your Fork](#push-to-your-fork)
- [Create a Pull Request](#create-a-pull-request)
- [Merge into Base Repository](#merge-into-base-repository)
- [Recommended Daily Workflow](#recommended-daily-workflow)
- [Setup Requirements](#setup-requirements)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [Useful Git Commands](#useful-git-commands)

<a id="how-we-work"></a>
<details open>
<summary><strong>How We Work</strong></summary>

This section describes how we work on this project as a team.

We do not work directly on the base repository `main` branch. Instead, we use a fork-based workflow so every contribution is visible through its own branch and merge history.

This is our team workflow:

1. Each team member forks the main repository.
2. Each team member clones their own fork to their local machine.
3. Work is done on a new branch created from the fork's `main` branch.
4. Changes are committed and pushed to the fork.
5. A pull request is created from the fork branch to the base repository `main` branch.
6. After review, the branch is merged into the base repository.

This means:

- nobody works directly on the base repository `main`
- each change is traceable to a branch
- the merge commit or pull request history clearly shows that the change came from a branch

[Back to top](#top)

</details>

<a id="local-project-setup"></a>
<details open>
<summary><strong>Local Project Setup</strong></summary>

This section is different from the team workflow above.

Here the goal is to show how to install dependencies, run the backend, and run the frontend on your local machine after cloning the repository.

- backend runs from the `backend` folder
- frontend runs from the `frontend` folder
- by default the frontend expects the backend at `http://localhost:8080`

[Back to top](#top)

</details>

<a id="initial-setup"></a>
<details>
<summary><strong>Initial Setup</strong></summary>

Before starting, make sure you have:

- Git installed
- Node.js and npm installed for the frontend
- Java installed for the backend
- Maven installed or use the Maven Wrapper included in the project
- a GitHub account with access to the repository

You should begin from the base repository:

```text
https://github.com/Neueda-Learning/11_105_CTRL-ALT-DEFEAT
```

[Back to top](#top)

</details>

<a id="clone-your-fork"></a>
<details>
<summary><strong>Clone Your Fork</strong></summary>

### Step 1: Fork the base repository on GitHub

Open the base repository in your browser and click `Fork`.

This creates your own copy of the repository under your GitHub account.

### Step 2: Clone your fork locally

Replace `YOUR_USERNAME` with your GitHub username:

```powershell
git clone https://github.com/YOUR_USERNAME/11_105_CTRL-ALT-DEFEAT.git
cd 11_105_CTRL-ALT-DEFEAT
```

[Back to top](#top)

</details>

<a id="keep-base-repo-as-upstream"></a>
<details>
<summary><strong>Keep Base Repo as Upstream</strong></summary>

After cloning your fork, add the original repository as `upstream`.

```powershell
git remote add upstream https://github.com/Neueda-Learning/11_105_CTRL-ALT-DEFEAT.git
git remote -v
```

Expected idea:

- `origin` points to your fork
- `upstream` points to the original team repository

[Back to top](#top)

</details>

<a id="create-a-branch"></a>
<details>
<summary><strong>Create a Branch</strong></summary>

Always create your working branch from your fork's updated `main` branch.

```powershell
git checkout main
git fetch upstream
git merge upstream/main
git push origin main
git checkout -b feature/your-change-name
```

Examples:

```powershell
git checkout -b feature/payment-history-ui
git checkout -b feature/readme-update
git checkout -b fix/api-validation
```

This is important because your work should happen on a branch, not directly on `main`.

[Back to top](#top)

</details>

<a id="make-changes-and-commit"></a>
<details>
<summary><strong>Make Changes and Commit</strong></summary>

After creating your branch:

1. Make your code or documentation changes locally.
2. Check what changed.
3. Commit the changes with a clear message.

Example:

```powershell
git status
git add .
git commit -m "Add frontend and backend README updates"
```

Use commit messages that describe what you changed.

[Back to top](#top)

</details>

<a id="push-to-your-fork"></a>
<details>
<summary><strong>Push to Your Fork</strong></summary>

Push the branch to your fork on GitHub:

```powershell
git push origin feature/your-change-name
```

Example:

```powershell
git push origin feature/readme-update
```

This sends your branch to your fork, not directly to the base repository.

[Back to top](#top)

</details>

<a id="create-a-pull-request"></a>
<details>
<summary><strong>Create a Pull Request</strong></summary>

After pushing your branch:

1. Open your fork on GitHub.
2. GitHub will usually show a `Compare & pull request` button.
3. Create a pull request from:

```text
YOUR_FORK: feature/your-change-name
```

to:

```text
Neueda-Learning/11_105_CTRL-ALT-DEFEAT: main
```

The pull request should explain:

- what was changed
- why it was changed
- anything reviewers should test or verify

[Back to top](#top)

</details>

<a id="merge-into-base-repository"></a>
<details open>
<summary><strong>Merge into Base Repository</strong></summary>

Once the pull request is reviewed and approved, it is merged into the base repository `main` branch.

This is the important part of your workflow:

- the change is visible as coming from a branch
- the commit history shows that the work was merged from a branch
- the base repository keeps a clear contribution trail

In GitHub, this is normally visible through:

- the pull request history
- the merge commit
- the branch name attached to the pull request

That means when someone looks at the commit or PR history, they can see that the work was not done directly on `main`, but merged from a feature branch.

[Back to top](#top)

</details>

<a id="recommended-daily-workflow"></a>
<details>
<summary><strong>Recommended Daily Workflow</strong></summary>

Use this workflow each time you start new work:

```powershell
git checkout main
git fetch upstream
git merge upstream/main
git push origin main
git checkout -b feature/new-task
```

Then after finishing:

```powershell
git add .
git commit -m "Describe your change"
git push origin feature/new-task
```

Then open a pull request from your fork branch into the base repository `main`.

[Back to top](#top)

</details>

<a id="setup-requirements"></a>
<details open>
<summary><strong>Setup Requirements</strong></summary>

To run the project locally, install the following:

- Git
- Java
- Node.js
- npm
- Docker Desktop if you want to run containers instead of local processes

Project folders:

- backend code: `backend/`
- frontend code: `frontend/`
- documentation: `readme/`

[Back to top](#top)

</details>

<a id="backend-setup"></a>
<details open>
<summary><strong>Backend Setup</strong></summary>

Run backend commands from the `backend` folder.

### Build the backend

```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
```

### Run the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend should start on:

```text
http://localhost:8080
```

### Run backend with Docker

```powershell
cd backend
docker-compose up --build
```

[Back to top](#top)

</details>

<a id="frontend-setup"></a>
<details open>
<summary><strong>Frontend Setup</strong></summary>

Run frontend commands from the `frontend` folder.

### Install frontend dependencies

```powershell
cd frontend
npm install
```

### Start frontend in development mode

```powershell
cd frontend
npm run dev
```

### Build frontend

```powershell
cd frontend
npm run build
```

### Preview production build

```powershell
cd frontend
npm run preview
```

### Frontend environment variable

The frontend uses:

```env
VITE_API_BASE_URL=http://localhost:8080
```

If you do not change it, the frontend will try to connect to the local backend on port `8080`.

[Back to top](#top)

</details>

<a id="useful-git-commands"></a>
<details>
<summary><strong>Useful Git Commands</strong></summary>

### Check remotes

```powershell
git remote -v
```

### See current branch

```powershell
git branch
```

### Fetch latest updates from base repository

```powershell
git fetch upstream
```

### Update your local main from upstream

```powershell
git checkout main
git merge upstream/main
```

### Push updated main to your fork

```powershell
git push origin main
```

### Create a new feature branch

```powershell
git checkout -b feature/my-task
```

### Push a branch for the first time

```powershell
git push origin feature/my-task
```

[Back to top](#top)

</details>