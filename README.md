# Horse Racing Management System - Backend

<p align="center">
  <b>Backend API for a multi-role horse racing management, betting, wallet, and referee operation system.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" />
  <img src="https://img.shields.io/badge/Spring%20Boot-API-success" />
  <img src="https://img.shields.io/badge/Database-SQL%20Server-blue" />
  <img src="https://img.shields.io/badge/Auth-JWT-yellow" />
  <img src="https://img.shields.io/badge/Status-In%20Development-purple" />
</p>

---

## Overview

Horse Racing Management System is a backend service for managing the full lifecycle of a horse racing platform.

The system supports multiple roles:

| Role | Main Responsibility |
|---|---|
| Admin | Approves users, manages accounts, deposits, complaints, and system data |
| Organizer | Creates tournaments, rounds, races, opens registration, assigns referees, approves results |
| Horse Owner | Manages horses, registers race entries, invites jockeys, sends complaints |
| Jockey | Receives invitations and participates in races |
| Referee | Checks race readiness, records results, violations, and race minutes |
| Spectator | Views races, places bets, manages wallet, and follows leaderboards |

---

## Core Modules

### Authentication & Account Approval
- Register and login
- JWT access token and refresh token
- Google login for Spectator
- Admin approval for non-spectator accounts
- Forgot/reset/change password

### Tournament & Race Management
- Create tournaments
- Create rounds: Qualify, Semi Final, Final
- Create races inside rounds
- Open race registration
- Update race status:
  - Draft
  - RegistrationOpen
  - Ongoing
  - Finished
  - Cancelled

### Horse Owner Flow
- Manage horse profiles
- Update horse status
- Register horses into races
- Invite jockeys
- Pay jockey after deal confirmation
- Receive prize money after tournament/race result publication

### Jockey Flow
- Receive jockey invitations
- Accept or decline invitations
- Join approved race entries

### Referee Flow
- View assigned races only
- Receive assignment notifications
- Perform pre-race checks
- Verify valid race entries, horses, and jockeys
- Record race results
- Record violations
- Let the system recalculate official ranking
- Create race minutes
- Support owner complaints after race

### Betting & Wallet Flow
- Spectator wallet
- Deposit request with transfer code and QR
- Admin approves/rejects deposits
- Auto reject pending deposit after demo timeout
- Place single bets
- Place combo/parlay bets
- View wallet transaction history
- View betting history

### Notification & Dashboard
- Role-based notifications
- Unread notification count
- Shared dashboard data
- Leaderboard preview
- Role quick actions

---

## Business Flow Summary

```text
User Register/Login
        |
        v
Admin approves role accounts
        |
        v
Organizer creates Tournament -> Round -> Race
        |
        v
Organizer opens race registration
        |
        v
Horse Owner registers horse entry
        |
        v
Organizer approves entry
        |
        v
Owner invites Jockey -> Jockey accepts
        |
        v
Organizer assigns Referee
        |
        v
Referee performs pre-race check
        |
        v
Race starts -> Referee records result and violations
        |
        v
System recalculates ranking
        |
        v
Organizer approves and publishes result
        |
        v
Prize, betting settlement, leaderboard, and complaints
