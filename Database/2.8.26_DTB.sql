-- ============================================================
--  HORSE RACING TOURNAMENT MANAGEMENT SYSTEM — DATABASE v2
--  MS SQL Server - synchronized with Business Flow v3 and backend fixes
--  Replaces DB26.6.2026 for dev drop-and-recreate usage
-- ============================================================
--  MAIN CHANGES from the previous version:
--   1. One Organizer role only; OrganizerHead/OrganizerMember removed
--   2. Tournament: PendingApproval status; Organizer creates and Admin approves
--   3. RaceEntries: OrganizerApproved; redundant OwnerConfirmed/AdminApproved removed
--   4. RaceResults: added PenaltyTime and computed FinalTime; ranking uses FinalTime
--   5. Violations: PenaltySeconds and EvidenceImageURL
--   6. RaceMinutes: added MinutesFileURL for signed minutes evidence
--   7. Races.Status: added RegistrationOpen
--   8. Stored procedures settle bets on publish, handle DQ, credit owner prize wallets, and auto-create wallets
--   9. Horse HealthStatus is updated only by Organizer/BTC using HealthUpdatedBy
-- ============================================================

-- Required ON options for computed columns.
-- Keep them at the top so this script runs consistently in SSMS, sqlcmd, and other clients.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
USE master;
GO
IF EXISTS (SELECT name FROM sys.databases WHERE name = N'HorseRacingDB')
BEGIN
    ALTER DATABASE HorseRacingDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE HorseRacingDB;
END
GO
CREATE DATABASE HorseRacingDB COLLATE Vietnamese_CI_AS;
GO
USE HorseRacingDB;
GO

-- ============================================================
-- 1. ROLES & USERS
-- ============================================================
CREATE TABLE Roles (
    RoleID      INT IDENTITY(1,1) PRIMARY KEY,
    RoleName    NVARCHAR(50)  NOT NULL UNIQUE,   -- Admin, HorseOwner, Jockey, Referee, Spectator, Organizer
    Description NVARCHAR(255),
    CreatedAt   DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE Users (
    UserID        INT IDENTITY(1,1) PRIMARY KEY,
    Username      NVARCHAR(100)  NOT NULL UNIQUE,
    PasswordHash  NVARCHAR(256)  NOT NULL,          -- BCrypt only; never store plaintext
    FullName      NVARCHAR(150)  NOT NULL,
    Email         NVARCHAR(150)  NOT NULL UNIQUE,
    Phone         NVARCHAR(20),
    AvatarURL     NVARCHAR(500),
    RoleID        INT            NOT NULL REFERENCES Roles(RoleID),   -- 1 user = 1 role
    IsActive      BIT            NOT NULL DEFAULT 1,
    IsApproved    BIT            NOT NULL DEFAULT 0,   -- Admin account approval
    FailedLoginAttempts INT      NOT NULL DEFAULT 0,
    IsLocked      BIT            NOT NULL DEFAULT 0,
    LastLogin     DATETIME2      NULL,
    ResetToken    NVARCHAR(255)  NULL,
    ResetTokenExpiry DATETIME2   NULL,
    RejectReason  NVARCHAR(500)  NULL,          -- (1) Admin rejection reason
    IsSystemAdmin BIT            NOT NULL DEFAULT 0,  -- (2) Hard Admin cannot be demoted, locked, or deleted
    CreatedAt     DATETIME2      DEFAULT GETDATE(),
    UpdatedAt     DATETIME2      DEFAULT GETDATE()
);

CREATE TABLE Permissions (
    PermissionID   INT IDENTITY(1,1) PRIMARY KEY,
    PermissionName NVARCHAR(100) NOT NULL UNIQUE,
    Description    NVARCHAR(255)
);
CREATE TABLE RolePermissions (
    RoleID       INT NOT NULL REFERENCES Roles(RoleID),
    PermissionID INT NOT NULL REFERENCES Permissions(PermissionID),
    PRIMARY KEY (RoleID, PermissionID)
);

-- Refresh tokens (JWT). userId is extracted server-side from JWT; do not use X-User-Id header.
CREATE TABLE UserTokens (
    TokenID   INT IDENTITY(1,1) PRIMARY KEY,
    UserID    INT           NOT NULL REFERENCES Users(UserID),
    Token     NVARCHAR(512) NOT NULL,
    ExpiresAt DATETIME2     NOT NULL,
    IsRevoked BIT           NOT NULL DEFAULT 0,
    CreatedAt DATETIME2     DEFAULT GETDATE()
);

CREATE TABLE UserRoleHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    UserID    INT NOT NULL REFERENCES Users(UserID),
    OldRoleID INT NULL REFERENCES Roles(RoleID),
    NewRoleID INT NOT NULL REFERENCES Roles(RoleID),
    ChangedBy INT NULL REFERENCES Users(UserID),
    ChangedAt DATETIME2 DEFAULT GETDATE()
);

-- ============================================================
-- 2. PROFILES: HorseOwner / Jockey / Referee
--    Owner-as-jockey flow removed: each user has one role only.
-- ============================================================
CREATE TABLE HorseOwners (
    OwnerID       INT IDENTITY(1,1) PRIMARY KEY,
    UserID        INT           NOT NULL UNIQUE REFERENCES Users(UserID),
    NationalID    NVARCHAR(30)  NOT NULL,
    Address       NVARCHAR(300),
    Organization  NVARCHAR(200),
    LicenseNumber NVARCHAR(50),
    CreatedAt     DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE Jockeys (
    JockeyID       INT IDENTITY(1,1) PRIMARY KEY,
    UserID         INT           NOT NULL UNIQUE REFERENCES Users(UserID),
    NationalID     NVARCHAR(30)  NOT NULL,
    LicenseNumber  NVARCHAR(50)  NOT NULL,
    WeightKg       DECIMAL(5,2),
    HeightCm       DECIMAL(5,2),
    ExperienceYear INT           DEFAULT 0,
    ApprovalStatus NVARCHAR(30)  NOT NULL DEFAULT 'Approved', -- Admin approves jockey profile
    TotalRaces     INT           DEFAULT 0,
    TotalWins      INT           DEFAULT 0,
    CreatedAt      DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE Referees (
    RefereeID   INT IDENTITY(1,1) PRIMARY KEY,
    UserID      INT           NOT NULL UNIQUE REFERENCES Users(UserID),
    BadgeNumber NVARCHAR(50)  NOT NULL,
    Speciality  NVARCHAR(150),
    CreatedAt   DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 3. HORSES  (HealthStatus: Active / Injured / Inactive)
-- ============================================================
CREATE TABLE Horses (
    HorseID       INT IDENTITY(1,1) PRIMARY KEY,
    OwnerID       INT           NOT NULL REFERENCES HorseOwners(OwnerID),
    HorseName     NVARCHAR(150) NOT NULL,
    Breed         NVARCHAR(100),
    BirthYear     INT,
    Color         NVARCHAR(50),
    Gender        NVARCHAR(20),
    WeightKg      DECIMAL(6,2),
    RegisterCode  NVARCHAR(50),                 -- generated by backend
    HealthStatus  NVARCHAR(100) NOT NULL DEFAULT N'Active',
    HealthUpdatedBy INT         NULL REFERENCES Users(UserID),  -- only Organizer/BTC updates after offline health check
    HealthUpdatedAt DATETIME2   NULL,
    PhotoURL      NVARCHAR(500),
    IsActive      BIT           NOT NULL DEFAULT 1,
    CreatedAt     DATETIME2     DEFAULT GETDATE(),
    UpdatedAt     DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE HorseHealthRecords (
    RecordID   INT IDENTITY(1,1) PRIMARY KEY,
    HorseID    INT           NOT NULL REFERENCES Horses(HorseID),
    CheckDate  DATE          NOT NULL,
    VetName    NVARCHAR(150),
    HealthStatus NVARCHAR(100),
    Diagnosis  NVARCHAR(500),
    Notes      NVARCHAR(1000),
    RecordedBy INT           NULL REFERENCES Users(UserID),  -- Organizer/BTC records health check
    CreatedAt  DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 4. TOURNAMENTS & ROUNDS
--    Organizer creates (CreatedBy) -> sends to Admin for approval (ApprovedByAdmin)
-- ============================================================
CREATE TABLE Tournaments (
    TournamentID   INT IDENTITY(1,1) PRIMARY KEY,
    TournamentName NVARCHAR(200) NOT NULL,
    Description    NVARCHAR(1000),
    Location       NVARCHAR(300),
    StartDate      DATE          NOT NULL,
    EndDate        DATE          NOT NULL,
    BudgetTotal    DECIMAL(18,2) DEFAULT 0,   -- (8) renamed PrizeFund to BudgetTotal to match API
    MaxHorses      INT           NULL,
    MaxParticipants INT          NULL,        -- (8) added to match API maxParticipants
    Status         NVARCHAR(30)  NOT NULL DEFAULT 'Draft',
    -- Draft | Open | Ongoing | Finished | Cancelled
    CreatedBy      INT           REFERENCES Users(UserID),   -- created by Organizer
    ApprovedByAdmin INT          NULL REFERENCES Users(UserID),
    ApprovedAt     DATETIME2     NULL,
    RejectReason   NVARCHAR(500) NULL,
    CreatedAt      DATETIME2     DEFAULT GETDATE(),
    UpdatedAt      DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE Rounds (
    RoundID      INT IDENTITY(1,1) PRIMARY KEY,
    TournamentID INT           NOT NULL REFERENCES Tournaments(TournamentID),
    RoundName    NVARCHAR(100) NOT NULL,
    RoundOrder   INT           NOT NULL,
    StartDate    DATE,
    EndDate      DATE,
    Description  NVARCHAR(500),
    CreatedAt    DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 5. RACES  (Status: Draft | RegistrationOpen | Ongoing | Finished | Cancelled)
--    Referee updates race status; app layer controls allowed transitions.
-- ============================================================
CREATE TABLE Races (
    RaceID          INT IDENTITY(1,1) PRIMARY KEY,
    TournamentID    INT           NOT NULL REFERENCES Tournaments(TournamentID),
    RoundID         INT           REFERENCES Rounds(RoundID),
    RaceName        NVARCHAR(200) NOT NULL,
    RaceDate        DATETIME2     NOT NULL,
    TrackLength     INT,            -- meters
    TrackType       NVARCHAR(50),
    MaxParticipants INT,
    PrizeFirst      DECIMAL(18,2)  DEFAULT 0,
    PrizeSecond     DECIMAL(18,2)  DEFAULT 0,
    PrizeThird      DECIMAL(18,2)  DEFAULT 0,
    Status          NVARCHAR(30)   NOT NULL DEFAULT 'Draft',
    RegistrationOpen  DATETIME2    NULL,
    RegistrationClose DATETIME2    NULL,
    CreatedAt       DATETIME2      DEFAULT GETDATE(),
    UpdatedAt       DATETIME2      DEFAULT GETDATE()
);
-- Referee assignments for races, assigned by Organizer.
CREATE TABLE RaceReferees (
    RaceRefereeID INT IDENTITY(1,1) PRIMARY KEY,
    RaceID        INT NOT NULL REFERENCES Races(RaceID),
    RefereeID     INT NOT NULL REFERENCES Referees(RefereeID),
    Role          NVARCHAR(50),   -- Chief / Assistant
    AssignedAt    DATETIME2 DEFAULT GETDATE(),
    UNIQUE (RaceID, RefereeID)
);
-- (7) Race status history: stores who changed the status and when.
CREATE TABLE RaceStatusHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    RaceID    INT NOT NULL REFERENCES Races(RaceID),
    OldStatus NVARCHAR(30),
    NewStatus NVARCHAR(30) NOT NULL,
    ChangedBy INT REFERENCES Users(UserID),
    ChangedAt DATETIME2 DEFAULT GETDATE()
);

-- ============================================================
-- 6. RACE ENTRIES  (Organizer approval: OrganizerApproved)
-- ============================================================
CREATE TABLE RaceEntries (
    EntryID            INT IDENTITY(1,1) PRIMARY KEY,
    RaceID             INT NOT NULL REFERENCES Races(RaceID),
    HorseID            INT NOT NULL REFERENCES Horses(HorseID),
    JockeyID           INT NULL REFERENCES Jockeys(JockeyID),
    LaneNumber         INT,
    RegistrationStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending', -- source of truth: Pending|Approved|Rejected|Withdrawn|Approved Without Jockey|Ready|PreRaceRejected
    OrganizerApproved  BIT          NOT NULL DEFAULT 0,          -- helper flag; backend must keep it in sync with RegistrationStatus
    ApprovedBy         INT          NULL REFERENCES Users(UserID),  -- approver user
    RejectReason       NVARCHAR(500) NULL,
    JockeyConfirmed    BIT          NOT NULL DEFAULT 0,          -- jockey accepted invitation; backend can set Ready
    Odds               DECIMAL(10,2) NOT NULL DEFAULT 2.00,     -- (5) server-side odds snapshot
    RegisteredAt       DATETIME2    DEFAULT GETDATE(),
    UpdatedAt          DATETIME2    DEFAULT GETDATE(),
    UNIQUE (RaceID, HorseID)
);

-- ============================================================
-- 7. JOCKEY INVITATIONS
-- ============================================================
CREATE TABLE JockeyInvitations (
    InvitationID   INT IDENTITY(1,1) PRIMARY KEY,
    EntryID        INT           NOT NULL REFERENCES RaceEntries(EntryID),
    JockeyID       INT           NOT NULL REFERENCES Jockeys(JockeyID),
    InvitedByOwner INT           NOT NULL REFERENCES HorseOwners(OwnerID),
    Message        NVARCHAR(500),
    DealAmount     DECIMAL(18,2) NOT NULL DEFAULT 0,
    RejectReason   NVARCHAR(500) NULL,
    Status         NVARCHAR(20)  NOT NULL DEFAULT 'Pending',  -- Pending|Accepted|Declined
    InvitedAt      DATETIME2     DEFAULT GETDATE(),
    RespondedAt    DATETIME2
);

-- ============================================================
-- 8. PRE-RACE CHECKS
-- ============================================================
CREATE TABLE PreRaceChecks (
    PreRaceCheckID INT IDENTITY(1,1) PRIMARY KEY,
    RaceID         INT          NOT NULL,
    EntryID        INT          NOT NULL,
    HorseID        INT          NOT NULL,
    RefereeID      INT          NOT NULL,
    Status         NVARCHAR(20) NOT NULL DEFAULT 'Pending'
        CHECK (Status IN ('Pending', 'Checked', 'Rejected')),
    Reason         NVARCHAR(500) NULL,
    CheckedAt      DATETIME2     NULL,
    CONSTRAINT UQ_PreRaceChecks_Race_Entry UNIQUE (RaceID, EntryID),
    FOREIGN KEY (RaceID) REFERENCES Races(RaceID),
    FOREIGN KEY (EntryID) REFERENCES RaceEntries(EntryID),
    FOREIGN KEY (HorseID) REFERENCES Horses(HorseID),
    FOREIGN KEY (RefereeID) REFERENCES Referees(RefereeID)
);
GO

-- ============================================================
-- 9. RACE RESULTS
--    Referee enters FinishTime; system computes FinalTime = FinishTime + PenaltyTime.
--    FinishPosition ranks by ascending FinalTime; DQ/DNF are placed last.
-- ============================================================
CREATE TABLE RaceResults (
    ResultID       INT IDENTITY(1,1) PRIMARY KEY,
    RaceID         INT           NOT NULL REFERENCES Races(RaceID),
    EntryID        INT           NOT NULL REFERENCES RaceEntries(EntryID),
    FinishTime     DECIMAL(10,3) NULL,      -- seconds
    PenaltyTime    DECIMAL(10,3) NOT NULL DEFAULT 0,  -- total penalty seconds from Violations
    FinalTime      AS (CASE WHEN FinishTime IS NULL THEN NULL ELSE FinishTime + PenaltyTime END) PERSISTED,
    FinishPosition INT           NULL,      -- computed from FinalTime
    Points         INT           NOT NULL DEFAULT 0,
    PrizeWon       DECIMAL(18,2) NOT NULL DEFAULT 0,
    DNF            BIT           NOT NULL DEFAULT 0,
    DQ             BIT           NOT NULL DEFAULT 0,
    ConfirmedByRef INT           NULL REFERENCES Referees(RefereeID),
    ConfirmedAt    DATETIME2,
    ApprovalStatus NVARCHAR(30)  NOT NULL DEFAULT 'Pending', -- Pending|Approved|Rejected|Published
    ApprovedByOrganizer INT      NULL REFERENCES Users(UserID),
    ApprovedAt     DATETIME2,
    PublishedAt    DATETIME2,
    CreatedAt      DATETIME2     DEFAULT GETDATE(),
    UNIQUE (RaceID, EntryID)
);

-- ============================================================
-- 10. VIOLATIONS  (numeric PenaltySeconds + evidence image)
--    Rule: false start +3s, lane violation +5s, obstruction +10s, serious violation -> DQ
-- ============================================================
CREATE TABLE Violations (
    ViolationID      INT IDENTITY(1,1) PRIMARY KEY,
    RaceID           INT           NOT NULL REFERENCES Races(RaceID),
    EntryID          INT           NOT NULL REFERENCES RaceEntries(EntryID),
    RefereeID        INT           NOT NULL REFERENCES Referees(RefereeID),
    ViolationType    NVARCHAR(100) NOT NULL,   -- XuatPhatSai | LanLane | CanDuong | ViPhamNang
    PenaltySeconds   DECIMAL(5,2)  NOT NULL DEFAULT 0,  -- 0 when DQ only
    IsDQ             BIT           NOT NULL DEFAULT 0,
    EvidenceImageURL NVARCHAR(500),            -- evidence image
    Description      NVARCHAR(1000),
    RecordedAt       DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 11. RACE MINUTES  (signed minutes image/PDF + sent-to-owner flag)
-- ============================================================
CREATE TABLE RaceMinutes (
    MinuteID       INT IDENTITY(1,1) PRIMARY KEY,
    RaceID         INT           NOT NULL UNIQUE REFERENCES Races(RaceID),
    RefereeID      INT           NOT NULL REFERENCES Referees(RefereeID),
    Content        NVARCHAR(MAX),
    WeatherCondition NVARCHAR(200),
    PreRaceChecks  NVARCHAR(MAX),
    PostRaceNotes  NVARCHAR(MAX),
    MinutesFileURL NVARCHAR(500),   -- signed minutes image/PDF
    SentToOwners   BIT           NOT NULL DEFAULT 0,  -- whether minutes were sent to all owners
    SentAt         DATETIME2     NULL,
    CreatedAt      DATETIME2     DEFAULT GETDATE(),
    UpdatedAt      DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 12. LEADERBOARD / RANKING
-- ============================================================
CREATE TABLE JockeyTournamentStats (
    StatID       INT IDENTITY(1,1) PRIMARY KEY,
    TournamentID INT NOT NULL REFERENCES Tournaments(TournamentID),
    JockeyID     INT NOT NULL REFERENCES Jockeys(JockeyID),
    TotalRaces   INT DEFAULT 0, TotalWins INT DEFAULT 0, TotalPodiums INT DEFAULT 0,
    TotalPrize   DECIMAL(18,2) DEFAULT 0, Points INT DEFAULT 0,
    UpdatedAt    DATETIME2 DEFAULT GETDATE(),
    UNIQUE (TournamentID, JockeyID)
);
CREATE TABLE HorseTournamentStats (
    StatID       INT IDENTITY(1,1) PRIMARY KEY,
    TournamentID INT NOT NULL REFERENCES Tournaments(TournamentID),
    HorseID      INT NOT NULL REFERENCES Horses(HorseID),
    TotalRaces   INT DEFAULT 0, TotalWins INT DEFAULT 0, TotalPodiums INT DEFAULT 0,
    TotalPrize   DECIMAL(18,2) DEFAULT 0, Points INT DEFAULT 0,
    -- Aliases for BE HorseRepository.findHorseRank.
    -- Computed columns always match Points/TotalWins.
    TotalPoints  AS (Points),
    Wins         AS (TotalWins),
    UpdatedAt    DATETIME2 DEFAULT GETDATE(),
    UNIQUE (TournamentID, HorseID)
);

-- ============================================================
-- 13. WALLET + BETTING
-- ============================================================
CREATE TABLE Wallets (
    WalletID  INT IDENTITY(1,1) PRIMARY KEY,
    UserID    INT           NOT NULL UNIQUE REFERENCES Users(UserID),
    Balance   DECIMAL(18,2) NOT NULL DEFAULT 0,
    CreatedAt DATETIME2     DEFAULT GETDATE(),
    UpdatedAt DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE DepositRequests (
    DepositRequestID INT IDENTITY(1,1) PRIMARY KEY,
    UserID           INT           NOT NULL REFERENCES Users(UserID),
    WalletID         INT           NOT NULL REFERENCES Wallets(WalletID),
    Amount           DECIMAL(18,2) NOT NULL CHECK (Amount > 0),
    PaymentMethod    NVARCHAR(20)  NOT NULL CHECK (PaymentMethod IN ('BANK', 'MOMO')),
    TransferCode     NVARCHAR(50)  NOT NULL UNIQUE,
    QrCodeUrl        NVARCHAR(500),
    Status           NVARCHAR(20)  NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    AdminNote        NVARCHAR(500),
    ApprovedBy       INT           NULL REFERENCES Users(UserID),
    ApprovedAt       DATETIME2,
    CreatedAt        DATETIME2     DEFAULT GETDATE(),
    UpdatedAt        DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE DepositComplaints (
    ComplaintID      INT IDENTITY(1,1) PRIMARY KEY,
    UserID           INT           NOT NULL REFERENCES Users(UserID),
    DepositRequestID INT           NULL REFERENCES DepositRequests(DepositRequestID),
    TransferCode     NVARCHAR(50),
    Amount           DECIMAL(18,2) NOT NULL CHECK (Amount > 0),
    PaymentMethod    NVARCHAR(20)  NOT NULL CHECK (PaymentMethod IN ('BANK', 'MOMO')),
    Reason           NVARCHAR(1000) NOT NULL,
    EvidenceUrl      NVARCHAR(500),
    Status           NVARCHAR(20)  NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Resolved', 'Rejected')),
    AdminNote        NVARCHAR(500),
    ResolvedBy       INT           NULL REFERENCES Users(UserID),
    ResolvedAt       DATETIME2,
    CreatedAt        DATETIME2     DEFAULT GETDATE(),
    UpdatedAt        DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE WalletTransactions (
    TransactionID   INT IDENTITY(1,1) PRIMARY KEY,
    WalletID        INT           NOT NULL REFERENCES Wallets(WalletID),
    Amount          DECIMAL(18,2) NOT NULL,          -- sign convention: Deposit(+) BetPlaced(-) BetWon(+) BetRefund(+) PrizeAwarded(+)
    TransactionType NVARCHAR(50)  NOT NULL,          -- Deposit|BetPlaced|BetWon|BetRefund|PrizeAwarded
    Description     NVARCHAR(500),
    RelatedEntity   NVARCHAR(50),
    RelatedEntityID INT,
    CreatedAt       DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE Bets (
    BetID           INT IDENTITY(1,1) PRIMARY KEY,
    UserID          INT           NOT NULL REFERENCES Users(UserID),
    RaceID          INT           NOT NULL REFERENCES Races(RaceID),
    EntryID         INT           NOT NULL REFERENCES RaceEntries(EntryID),
    BetType         NVARCHAR(30)  NOT NULL DEFAULT 'WIN',   -- WIN | PLACE(top2) | SHOW(top3) | EXACT(position)
    TargetPosition  INT           NULL,                     -- used only for EXACT
    Amount          DECIMAL(18,2) NOT NULL,
    Odds            DECIMAL(10,2) NOT NULL DEFAULT 2.00,    -- odds snapshot at placement time
    PotentialPayout DECIMAL(18,2) NOT NULL,                 -- Amount * Odds
    Status          NVARCHAR(30)  NOT NULL DEFAULT 'Pending', -- Pending|Won|Lost|Cancelled
    CreatedAt       DATETIME2     DEFAULT GETDATE(),
    SettledAt       DATETIME2       -- UNIQUE(UserID,RaceID) removed to allow multiple bets per race
);

-- ============================================================
-- 13. NOTIFICATIONS  (supports bulk notifications for all owners)
-- ============================================================
CREATE TABLE Notifications (
    NotificationID  INT IDENTITY(1,1) PRIMARY KEY,
    UserID          INT           NOT NULL REFERENCES Users(UserID),
    Title           NVARCHAR(200) NOT NULL,
    Body            NVARCHAR(1000),
    NotifType       NVARCHAR(50),  -- (6) EntryPendingApproval|EntryApproved|EntryRejected|RefereeAssigned|InvitationReceived|InvitationDeclined|MinutesSent|ResultPublished|ResultRejected|PrizeAwarded|BetWon|BetRefund|RaceCancelled|SystemAlert
    RelatedEntityID INT,
    RelatedEntity   NVARCHAR(50),
    IsRead          BIT           NOT NULL DEFAULT 0,
    CreatedAt       DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 14. SYSTEM CONFIG + AUDIT
-- ============================================================
CREATE TABLE SystemConfigs (
    ConfigID    INT IDENTITY(1,1) PRIMARY KEY,
    ConfigKey   NVARCHAR(100) NOT NULL UNIQUE,
    ConfigValue NVARCHAR(1000),
    Description NVARCHAR(300),
    UpdatedBy   INT REFERENCES Users(UserID),
    UpdatedAt   DATETIME2 DEFAULT GETDATE()
);
CREATE TABLE AuditLogs (
    LogID     INT IDENTITY(1,1) PRIMARY KEY,
    UserID    INT           REFERENCES Users(UserID),
    Action    NVARCHAR(100) NOT NULL,
    TableName NVARCHAR(100),
    RecordID  INT,
    OldValue  NVARCHAR(MAX),
    NewValue  NVARCHAR(MAX),
    IPAddress NVARCHAR(50),
    CreatedAt DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX IX_Users_RoleID       ON Users(RoleID);
CREATE INDEX IX_Horses_OwnerID     ON Horses(OwnerID);
CREATE UNIQUE INDEX UX_Horses_RegisterCode ON Horses(RegisterCode) WHERE RegisterCode IS NOT NULL;
CREATE INDEX IX_Races_TournamentID ON Races(TournamentID);
CREATE INDEX IX_RaceEntries_RaceID ON RaceEntries(RaceID);
CREATE INDEX IX_RaceResults_RaceID ON RaceResults(RaceID);
CREATE INDEX IX_Violations_RaceID  ON Violations(RaceID);
CREATE INDEX IX_Bets_RaceID        ON Bets(RaceID);
CREATE INDEX IX_Bets_Status        ON Bets(Status);
CREATE INDEX IX_Notifications_User ON Notifications(UserID);
GO

-- ============================================================
-- SEED: Roles / Permissions / Configs
-- ============================================================
INSERT INTO Roles (RoleName, Description) VALUES
('Admin',      N'System administrator for users, tournaments, deposits, and approvals'),
('HorseOwner', N'Horse owner'),
('Jockey',     N'Jockey'),
('Referee',    N'Referee who updates race status, records results, violations, and minutes'),
('Spectator',  N'Spectator who views races and places bets'),
('Organizer',  N'Organizer who creates tournaments, approves entries, assigns referees, and publishes results');

INSERT INTO Permissions (PermissionName, Description) VALUES
('user.manage', N'Manage user accounts'),
('tournament.create', N'Create tournaments as Organizer'),
('tournament.approve', N'Approve tournaments as Admin'),
('entry.approve', N'Approve race entries as Organizer'),
('race.status.update', N'Update race status as Referee'),
('result.enter', N'Enter race results and violations as Referee'),
('result.approve', N'Approve race results as Organizer'),
('result.publish', N'Publish race results as Organizer'),
('referee.assign', N'Assign referees as Organizer'),
('horse.manage', N'Manage horses as Horse Owner'),
('health.update', N'Update horse health status as Organizer/BTC'),
('jockey.invite', N'Invite jockeys as Horse Owner'),
('bet.create', N'Place bets as Spectator'),
('config.manage', N'Manage system configuration as Admin');

-- Assign permissions
INSERT INTO RolePermissions (RoleID, PermissionID)
SELECT 1, PermissionID FROM Permissions;  -- Admin: all permissions
INSERT INTO RolePermissions (RoleID, PermissionID)
SELECT 2, PermissionID FROM Permissions WHERE PermissionName IN ('horse.manage','jockey.invite');
INSERT INTO RolePermissions (RoleID, PermissionID)
SELECT 4, PermissionID FROM Permissions WHERE PermissionName IN ('race.status.update','result.enter');
INSERT INTO RolePermissions (RoleID, PermissionID)
SELECT 5, PermissionID FROM Permissions WHERE PermissionName IN ('bet.create');
INSERT INTO RolePermissions (RoleID, PermissionID)
SELECT 6, PermissionID FROM Permissions
WHERE PermissionName IN ('tournament.create','entry.approve','result.approve','result.publish','referee.assign','health.update');

INSERT INTO SystemConfigs (ConfigKey, ConfigValue, Description) VALUES
('MAINTENANCE_MODE',  '0',    N'1 = maintenance mode, 0 = active'),
('MAINTENANCE_UNTIL', NULL,   N'Expected reopening time'),
('DEFAULT_BET_ODDS',  '2.00', N'Default betting odds'),
('PENALTY_XuatPhatSai','3',   N'False start penalty in seconds'),
('PENALTY_LanLane',   '5',    N'Lane violation penalty in seconds'),
('PENALTY_CanDuong',  '10',   N'Obstruction penalty in seconds'),
('ODDS_FACTOR_PLACE', '0.50', N'PLACE odds factor for top 2 bets'),
('ODDS_FACTOR_SHOW',  '0.35', N'SHOW odds factor for top 3 bets'),
('ODDS_FACTOR_EXACT', '1.50', N'EXACT position odds factor'),
('ODDS_MIN',          '1.10', N'Minimum odds for all bet types');
GO

-- ============================================================
-- STORED PROCEDURES
-- ============================================================

-- Admin approves account -> automatically creates wallet
CREATE PROCEDURE sp_ApproveUser @AdminID INT, @UserID INT, @Approve BIT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Users SET IsApproved = @Approve, UpdatedAt = GETDATE() WHERE UserID = @UserID;
    IF @Approve = 1 AND NOT EXISTS (SELECT 1 FROM Wallets WHERE UserID = @UserID)
        INSERT INTO Wallets (UserID, Balance) VALUES (@UserID, 0);
    INSERT INTO AuditLogs (UserID, Action, TableName, RecordID, NewValue)
    VALUES (@AdminID, CASE WHEN @Approve=1 THEN 'APPROVE_USER' ELSE 'REJECT_USER' END, 'Users', @UserID, CAST(@Approve AS NVARCHAR));
END;
GO

-- Compute ranking by FinalTime; DQ/DNF are placed last. Assign Points and PrizeWon.
CREATE PROCEDURE sp_ComputeRaceRanking @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    -- Rank valid horses by ascending FinalTime.
    ;WITH Ranked AS (
        SELECT ResultID,
               ROW_NUMBER() OVER (ORDER BY FinalTime ASC) AS Pos
        FROM RaceResults
        WHERE RaceID = @RaceID AND DQ = 0 AND DNF = 0 AND FinalTime IS NOT NULL
    )
    UPDATE rr SET FinishPosition = r.Pos,
        Points   = CASE r.Pos WHEN 1 THEN 10 WHEN 2 THEN 7 WHEN 3 THEN 5 WHEN 4 THEN 3 WHEN 5 THEN 2 ELSE 1 END,
        PrizeWon = CASE r.Pos
                     WHEN 1 THEN (SELECT PrizeFirst  FROM Races WHERE RaceID=@RaceID)
                     WHEN 2 THEN (SELECT PrizeSecond FROM Races WHERE RaceID=@RaceID)
                     WHEN 3 THEN (SELECT PrizeThird  FROM Races WHERE RaceID=@RaceID)
                     ELSE 0 END
    FROM RaceResults rr JOIN Ranked r ON rr.ResultID = r.ResultID;
    -- DQ/DNF: no position, no points, no prize.
    UPDATE RaceResults SET FinishPosition = NULL, Points = 0, PrizeWon = 0
    WHERE RaceID = @RaceID AND (DQ = 1 OR DNF = 1);
END;
GO

-- Settle bets: winner is rank 1 by smallest FinalTime, excluding DQ/DNF.
CREATE PROCEDURE sp_SettleBets @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Now DATETIME2 = GETDATE(), @Outer INT = @@TRANCOUNT;
    -- FinishPosition must be computed before settling bets
    IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @RaceID AND FinishPosition IS NOT NULL) RETURN;

    BEGIN TRY
        IF @Outer = 0 BEGIN TRANSACTION;
        -- Evaluate each bet by BetType using the selected horse's FinishPosition
        UPDATE b
        SET Status = CASE
              WHEN rr.FinishPosition IS NULL                                   THEN 'Lost'  -- DQ/DNF loses all bet types
              WHEN b.BetType = 'WIN'   AND rr.FinishPosition = 1               THEN 'Won'    -- first place
              WHEN b.BetType = 'PLACE' AND rr.FinishPosition <= 2              THEN 'Won'    -- top 2
              WHEN b.BetType = 'SHOW'  AND rr.FinishPosition <= 3              THEN 'Won'    -- top 3
              WHEN b.BetType = 'EXACT' AND rr.FinishPosition = b.TargetPosition THEN 'Won'   -- exact position
              ELSE 'Lost' END,
            SettledAt = @Now
        FROM Bets b
        LEFT JOIN RaceResults rr ON rr.RaceID = b.RaceID AND rr.EntryID = b.EntryID
        WHERE b.RaceID = @RaceID AND b.Status = 'Pending';

        -- Credit winning payouts, grouped by user
        UPDATE w SET Balance = w.Balance + agg.Total, UpdatedAt = GETDATE()
        FROM Wallets w
        JOIN (SELECT UserID, SUM(PotentialPayout) AS Total FROM Bets
              WHERE RaceID = @RaceID AND Status = 'Won' AND SettledAt = @Now
              GROUP BY UserID) agg ON agg.UserID = w.UserID;

        INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
        SELECT w.WalletID, b.PotentialPayout, 'BetWon', N'Bet payout after race results were published', 'Bet', b.BetID
        FROM Bets b JOIN Wallets w ON w.UserID = b.UserID
        WHERE b.RaceID = @RaceID AND b.Status = 'Won' AND b.SettledAt = @Now;
        IF @Outer = 0 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @Outer = 0 AND @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- Credit prize money to owner wallets by each horse's PrizeWon
CREATE PROCEDURE sp_AwardOwnerPrizes @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Outer INT = @@TRANCOUNT;
    BEGIN TRY
        IF @Outer = 0 BEGIN TRANSACTION;
        -- Credit owner prize money, grouped by owner
        UPDATE w SET Balance = w.Balance + agg.Total, UpdatedAt = GETDATE()
        FROM Wallets w
        JOIN (SELECT ho.UserID, SUM(rr.PrizeWon) AS Total
              FROM HorseOwners ho
              JOIN Horses h  ON h.OwnerID = ho.OwnerID
              JOIN RaceEntries re ON re.HorseID = h.HorseID
              JOIN RaceResults rr ON rr.EntryID = re.EntryID
              WHERE rr.RaceID = @RaceID AND rr.PrizeWon > 0
              GROUP BY ho.UserID) agg ON agg.UserID = w.UserID;

        INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
        SELECT w.WalletID, rr.PrizeWon, 'PrizeAwarded', N'Race placement prize awarded', 'Race', @RaceID
        FROM Wallets w
        JOIN HorseOwners ho ON ho.UserID = w.UserID
        JOIN Horses h  ON h.OwnerID = ho.OwnerID
        JOIN RaceEntries re ON re.HorseID = h.HorseID
        JOIN RaceResults rr ON rr.EntryID = re.EntryID
        WHERE rr.RaceID = @RaceID AND rr.PrizeWon > 0;
        IF @Outer = 0 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @Outer = 0 AND @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- Organizer publishes results -> computes ranking, settles bets, and credits owner prizes
CREATE PROCEDURE sp_PublishRaceResult @RaceID INT, @OrganizerID INT
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @RaceID AND ApprovalStatus = 'Approved')
    BEGIN
        RAISERROR(N'Race results must be approved by Organizer before publishing.', 16, 1); RETURN;
    END

    DECLARE @Outer INT = @@TRANCOUNT;
    BEGIN TRY
        IF @Outer = 0 BEGIN TRANSACTION;
        EXEC sp_ComputeRaceRanking @RaceID;   -- 1. rank by FinalTime

        UPDATE RaceResults SET ApprovalStatus = 'Published', PublishedAt = GETDATE()  -- 2. publish
        WHERE RaceID = @RaceID AND ApprovalStatus = 'Approved';
        UPDATE Races SET Status = 'Finished', UpdatedAt = GETDATE() WHERE RaceID = @RaceID;

        EXEC sp_SettleBets @RaceID;        -- 3. settle bets on publish
        EXEC sp_AwardOwnerPrizes @RaceID;  -- 4. credit owner prize wallets

        -- 5. notify owners
        INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
        SELECT DISTINCT u.UserID, N'Race results published', N'The race results have been published. Please check the leaderboard.', 'ResultPublished', @RaceID, 'Race'
        FROM RaceEntries re
        JOIN Horses h ON re.HorseID = h.HorseID
        JOIN HorseOwners ho ON h.OwnerID = ho.OwnerID
        JOIN Users u ON ho.UserID = u.UserID
        WHERE re.RaceID = @RaceID;

        INSERT INTO AuditLogs (UserID, Action, TableName, RecordID) VALUES (@OrganizerID, 'PUBLISH_RESULT', 'Races', @RaceID);  -- 6. audit
        IF @Outer = 0 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @Outer = 0 AND @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- Referee sends race minutes to all owners who have horses in the race
CREATE PROCEDURE sp_SendMinutesToOwners @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
    SELECT DISTINCT u.UserID, N'Race minutes sent', N'The referee has sent the race minutes with signature evidence.', 'MinutesSent', @RaceID, 'Race'
    FROM RaceEntries re
    JOIN Horses h ON re.HorseID = h.HorseID
    JOIN HorseOwners ho ON h.OwnerID = ho.OwnerID
    JOIN Users u ON ho.UserID = u.UserID
    WHERE re.RaceID = @RaceID;
    UPDATE RaceMinutes SET SentToOwners = 1, SentAt = GETDATE() WHERE RaceID = @RaceID;
END;
GO

-- Cancel race -> refund all pending bets
CREATE PROCEDURE sp_CancelRaceAndRefundBets @RaceID INT, @ByUser INT, @Reason NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Old NVARCHAR(30), @Now DATETIME2 = GETDATE(), @Outer INT = @@TRANCOUNT;
    SELECT @Old = Status FROM Races WHERE RaceID = @RaceID;

    BEGIN TRY
        IF @Outer = 0 BEGIN TRANSACTION;
        UPDATE Races SET Status = 'Cancelled', UpdatedAt = GETDATE() WHERE RaceID = @RaceID;
        INSERT INTO RaceStatusHistory (RaceID, OldStatus, NewStatus, ChangedBy) VALUES (@RaceID, @Old, 'Cancelled', @ByUser);

        -- Refund pending bets to wallets, grouped by user
        UPDATE w SET Balance = w.Balance + agg.Total, UpdatedAt = GETDATE()
        FROM Wallets w
        JOIN (SELECT UserID, SUM(Amount) AS Total FROM Bets
              WHERE RaceID = @RaceID AND Status = 'Pending'
              GROUP BY UserID) agg ON agg.UserID = w.UserID;

        INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
        SELECT w.WalletID, b.Amount, 'BetRefund', N'Refund for cancelled race', 'Bet', b.BetID
        FROM Bets b JOIN Wallets w ON w.UserID = b.UserID
        WHERE b.RaceID = @RaceID AND b.Status = 'Pending';

        UPDATE Bets SET Status = 'Cancelled', SettledAt = @Now WHERE RaceID = @RaceID AND Status = 'Pending';

        INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
        SELECT DISTINCT b.UserID, N'Race cancelled', N'The race was cancelled and your bet amount has been refunded to your wallet.', 'RaceCancelled', @RaceID, 'Race'
        FROM Bets b WHERE b.RaceID = @RaceID;

        INSERT INTO AuditLogs (UserID, Action, TableName, RecordID, NewValue) VALUES (@ByUser, 'CANCEL_RACE', 'Races', @RaceID, @Reason);
        IF @Outer = 0 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @Outer = 0 AND @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- ============================================================
-- TRIGGER: automatically updates PenaltyTime and DQ from Violations.
--   FinalTime is computed automatically; backend does not need manual addition.
--   PenaltySeconds stores seconds; IsDQ=1 means serious violation -> DQ.
-- ============================================================
CREATE TRIGGER trg_Violations_Recalc ON Violations
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    ;WITH Affected AS (
        SELECT EntryID FROM inserted
        UNION
        SELECT EntryID FROM deleted
    )
    UPDATE rr
    SET PenaltyTime = ISNULL(v.TotalPen, 0),
        DQ = CASE WHEN ISNULL(v.HasDQ, 0) = 1 THEN 1 ELSE 0 END
    FROM RaceResults rr
    JOIN Affected a ON a.EntryID = rr.EntryID
    OUTER APPLY (
        SELECT SUM(PenaltySeconds) AS TotalPen, MAX(CAST(IsDQ AS INT)) AS HasDQ
        FROM Violations WHERE EntryID = rr.EntryID
    ) v;
END;
GO

-- ============================================================
-- VIEWS
-- ============================================================
CREATE VIEW vw_JockeyLeaderboard AS
SELECT j.JockeyID, u.FullName AS JockeyName,
       SUM(s.TotalRaces) AS TotalRaces, SUM(s.TotalWins) AS TotalWins,
       SUM(s.TotalPodiums) AS TotalPodiums, SUM(s.Points) AS Points, SUM(s.TotalPrize) AS TotalPrize
FROM Jockeys j JOIN Users u ON j.UserID = u.UserID
JOIN JockeyTournamentStats s ON j.JockeyID = s.JockeyID
GROUP BY j.JockeyID, u.FullName;
GO
CREATE VIEW vw_HorseLeaderboard AS
SELECT h.HorseID, h.HorseName, u.FullName AS OwnerName,
       SUM(s.TotalRaces) AS TotalRaces, SUM(s.TotalWins) AS TotalWins,
       SUM(s.TotalPodiums) AS TotalPodiums, SUM(s.Points) AS Points, SUM(s.TotalPrize) AS TotalPrize
FROM Horses h
JOIN HorseOwners ho ON h.OwnerID = ho.OwnerID
JOIN Users u ON ho.UserID = u.UserID
JOIN HorseTournamentStats s ON h.HorseID = s.HorseID
GROUP BY h.HorseID, h.HorseName, u.FullName;
GO
CREATE VIEW vw_RaceResultDetail AS
SELECT rr.ResultID, r.RaceID, r.RaceName, t.TournamentName,
       h.HorseName, uo.FullName AS OwnerName, uj.FullName AS JockeyName,
       rr.FinishTime, rr.PenaltyTime, rr.FinalTime, rr.FinishPosition,
       rr.Points, rr.PrizeWon, rr.DNF, rr.DQ, rr.ApprovalStatus
FROM RaceResults rr
JOIN RaceEntries re ON rr.EntryID = re.EntryID
JOIN Races r ON rr.RaceID = r.RaceID
JOIN Tournaments t ON r.TournamentID = t.TournamentID
JOIN Horses h ON re.HorseID = h.HorseID
JOIN HorseOwners ho ON h.OwnerID = ho.OwnerID
JOIN Users uo ON ho.UserID = uo.UserID
LEFT JOIN Jockeys j ON re.JockeyID = j.JockeyID
LEFT JOIN Users uj ON j.UserID = uj.UserID;
GO
-- Admin dashboard summary; DashboardService reads these 9 columns.
CREATE VIEW vw_SystemDashboard AS
SELECT
  (SELECT COUNT(*) FROM Users   WHERE IsActive = 1)                                        AS TotalActiveUsers,
  (SELECT COUNT(*) FROM Users   WHERE IsApproved = 0)                                      AS PendingApprovals,
  (SELECT COUNT(*) FROM Tournaments WHERE Status = 'Ongoing')                              AS OngoingTournaments,
  (SELECT COUNT(*) FROM Races   WHERE Status IN ('RegistrationOpen','Ongoing')) AS UpcomingRaces,
  (SELECT COUNT(*) FROM Races   WHERE Status = 'Finished')                                 AS FinishedRaces,
  (SELECT COUNT(*) FROM Horses)                                                            AS TotalHorses,
  (SELECT COUNT(*) FROM Jockeys)                                                           AS TotalJockeys,
  (SELECT COUNT(*) FROM Bets)                                                              AS TotalBets,
  (SELECT COUNT(*) FROM Bets    WHERE Status = 'Won')                                      AS WonBets;
GO

-- ============================================================
-- DEMO SEED for main flows.
-- ============================================================
-- Password for all demo users below: 123456
-- Real BCrypt hash generated by backend; login works immediately.
DECLARE @Pwd NVARCHAR(100) = '$2a$10$6rvu1cSRS60NNTQtJQZpYO34ZCaJ73I8dFDvXdw4BxYzrlKFKhTq6';

INSERT INTO Users (Username, PasswordHash, FullName, Email, Phone, RoleID, IsActive, IsApproved) VALUES
('admin',      @Pwd, N'System Admin',        'admin@gmail.com',      '0900000001', 1, 1, 1),
('organizer1', @Pwd, N'Organizer A',        'organizer1@gmail.com', '0900000002', 6, 1, 1),
('owner1',     @Pwd, N'Owner A',            'owner1@gmail.com',     '0900000003', 2, 1, 1),
('owner2',     @Pwd, N'Owner B',            'owner2@gmail.com',     '0900000004', 2, 1, 1),
('jockey1',    @Pwd, N'Jockey C',           'jockey1@gmail.com',    '0900000005', 3, 1, 1),
('jockey2',    @Pwd, N'Jockey D',           'jockey2@gmail.com',    '0900000006', 3, 1, 1),
('referee1',   @Pwd, N'Referee E',          'referee1@gmail.com',   '0900000007', 4, 1, 1),
('spectator1', @Pwd, N'Spectator A',        'spectator1@gmail.com', '0900000008', 5, 1, 1),
('owner_pending', @Pwd, N'Pending Owner',   'pending@gmail.com',    '0900000009', 2, 1, 0);

INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber) VALUES
(3, '079205000001', N'Ho Chi Minh City', N'Black Stable', 'OWN001'),
(4, '079205000002', N'Da Nang',          N'Golden Stable','OWN002');
DECLARE @OwnerPendingUserID INT = (SELECT UserID FROM Users WHERE Username = 'owner_pending');
IF @OwnerPendingUserID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM HorseOwners WHERE UserID = @OwnerPendingUserID)
BEGIN
    INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber)
    VALUES (@OwnerPendingUserID, '079205000009', N'Ho Chi Minh City', N'Pending Stable', 'OWN-PENDING');
END;
INSERT INTO Jockeys (UserID, NationalID, LicenseNumber, WeightKg, HeightCm, ExperienceYear) VALUES
(5, '079205000003', 'JK001', 55.5, 170, 5),
(6, '079205000004', 'JK002', 53.0, 168, 4);
INSERT INTO Referees (UserID, BadgeNumber, Speciality) VALUES
(7, 'REF001', N'Chief Referee');

-- Horses for owner1 and owner2.
-- HealthStatus uses the same English values as backend and frontend labels.
INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive) VALUES
(1, N'Hac Phong', N'Thoroughbred', 2020, N'Black',      N'Male',   450.5, 'HP001', N'Active', 1),  -- HorseID 1
(1, N'Bao Lua',   N'Arabian',      2019, N'Brown',      N'Male',   430.0, 'BL002', N'Active', 1),  -- 2
(2, N'Tia Chop',  N'Mustang',      2021, N'White',      N'Female', 410.0, 'TC003', N'Active', 1),  -- 3
(1, N'Than Ma',   N'Warmblood',    2020, N'Gray',       N'Male',   445.0, 'TM004', N'Active', 1),  -- 4 owner1 has not registered yet
(2, N'Kim Long',  N'Andalusian',   2019, N'Golden',     N'Female', 420.0, 'KL005', N'Active', 1),  -- 5
(1, N'Xich Tho',  N'Thoroughbred', 2018, N'Reddish Brown', N'Male', 460.0, 'XT006', N'Active', 1);  -- 6

-- Mark hard Admin account.
UPDATE Users SET IsSystemAdmin = 1 WHERE Username = 'admin';

-- Wallets: spectators have demo betting balance; owners have demo operating balance.
INSERT INTO Wallets (UserID, Balance)
SELECT UserID, CASE WHEN RoleID = 5 THEN 2000000 WHEN RoleID = 2 THEN 1000000 ELSE 0 END
FROM Users WHERE IsApproved = 1;

-- Tournaments: T1 is open and approved; T2 is pending Admin approval.
-- Relative dates keep demo races in the future whenever the seed is run.
INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt) VALUES
(N'Summer Racing Cup 2026', N'Summer horse racing tournament.',  N'Ho Chi Minh City',
 DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,14,CAST(GETDATE() AS DATE)),
 50000000, 20, 20, 'Open', 2, 1, GETDATE()),                                                        -- TournamentID 1
(N'Autumn Racing Cup 2026', N'Submitted by Organizer and waiting for Admin approval.', N'Da Nang',
 DATEADD(DAY,20,CAST(GETDATE() AS DATE)), DATEADD(DAY,35,CAST(GETDATE() AS DATE)),
 40000000, 16, 16, 'PendingApproval', 2, NULL, NULL);                                               -- 2 pending Admin approval

INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate) VALUES
(1, N'Qualifying Round', 1, DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,7,CAST(GETDATE() AS DATE)));  -- RoundID 1

-- Two open-registration races in T1.
INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose) VALUES
(1, 1, N'Race Opening', DATEADD(DAY,3,CAST(GETDATE() AS DATE)), 1200, N'Flat', 10, 20000000, 10000000, 5000000, 'RegistrationOpen',
 DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,2,CAST(GETDATE() AS DATE))),                   -- RaceID 1
(1, 1, N'Race Sprint 800m', DATEADD(DAY,5,CAST(GETDATE() AS DATE)), 800, N'Flat', 8, 12000000, 6000000, 3000000, 'RegistrationOpen',
 DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,4,CAST(GETDATE() AS DATE)));                   -- RaceID 2

-- Race 1 entries: enough states to test all roles.
INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed) VALUES
(1, 1, 1,    1, 'Approved', 1, 2, 1),   -- EntryID 1: ready for betting
(1, 3, 2,    2, 'Approved', 1, 2, 1),   -- 2: ready for betting
(1, 2, NULL, 3, 'Approved', 1, 2, 0),   -- 3: approved but no jockey yet
(1, 5, NULL, 4, 'Pending',  0, NULL, 0),-- 4: waiting for Organizer approval
(1, 6, NULL, 5, 'Approved', 1, 2, 0);   -- 5: pending invitation sent to jockey2

-- Organizer assigns referee to Race 1.
INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (1, 1, N'Chief');

-- Pending jockey invitation for jockey2.
INSERT INTO JockeyInvitations (EntryID, JockeyID, InvitedByOwner, Message, Status) VALUES
(5, 2, 1, N'Owner A invited you to ride Xich Tho in Race Opening.', 'Pending');

-- Sample role-based notifications.
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity) VALUES
(7, N'Referee assignment received', N'You are the chief referee for Race Opening.', 'RefereeAssigned', 1, 'Race'),
(6, N'Race invitation received',     N'Owner A invited you to ride Xich Tho.',       'InvitationReceived', 5, 'Entry'),
(2, N'Entry pending approval',       N'Kim Long from owner2 is waiting for your approval.', 'EntryPendingApproval', 4, 'Entry');

-- Race 3: finished with results for replay/demo.
-- Kim Long has the fastest finish time but is DQ, so raw finish order differs from official ranking.
INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose) VALUES
(1, 1, N'Race Final', DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), 1400, N'Flat', 10, 30000000, 15000000, 8000000, 'Finished',
 DATEADD(DAY,-10,CAST(GETDATE() AS DATE)), DATEADD(DAY,-2,CAST(GETDATE() AS DATE)));   -- RaceID 3

DECLARE @R3 INT = (SELECT RaceID FROM Races WHERE RaceName = N'Race Final');

INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed) VALUES
(@R3, 1, 1,    1, 'Approved', 1, 2, 1),   -- Hac Phong / Jockey C
(@R3, 2, 2,    2, 'Approved', 1, 2, 1),   -- Bao Lua / Jockey D
(@R3, 3, NULL, 3, 'Approved', 1, 2, 0),   -- Tia Chop
(@R3, 4, NULL, 4, 'Approved', 1, 2, 0),   -- Than Ma
(@R3, 5, NULL, 5, 'Approved', 1, 2, 0);   -- Kim Long will be DQ

-- Published results: FinishTime in seconds, ranking by FinalTime; Kim Long is DQ.
INSERT INTO RaceResults (RaceID, EntryID, FinishTime, FinishPosition, DQ, Points, PrizeWon, ApprovalStatus, PublishedAt)
SELECT re.RaceID, re.EntryID,
  CASE re.HorseID WHEN 1 THEN 67.200 WHEN 2 THEN 68.500 WHEN 3 THEN 69.100 WHEN 4 THEN 70.300 WHEN 5 THEN 66.800 END,
  CASE re.HorseID WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 3 WHEN 4 THEN 4 ELSE NULL END,
  CASE re.HorseID WHEN 5 THEN 1 ELSE 0 END,
  CASE re.HorseID WHEN 1 THEN 10 WHEN 2 THEN 7 WHEN 3 THEN 5 WHEN 4 THEN 3 ELSE 0 END,
  CASE re.HorseID WHEN 1 THEN 30000000 WHEN 2 THEN 15000000 WHEN 3 THEN 8000000 ELSE 0 END,
  'Published', GETDATE()
FROM RaceEntries re WHERE re.RaceID = @R3;


-- ============================================================
-- DEMO TEST DATA - 5 FLOWS CLEAN
-- Keep schema unchanged. This block only adds clear test data for 5 flows.
-- Demo account convention: password 123456.
-- Prefer SELECT by Username/RaceName instead of hard-coding IDs when demoing existing databases.
-- ============================================================

-- 1) Additional odds config from baseodds.txt for BettingService.
MERGE SystemConfigs AS target
USING (VALUES
('ODDS_BASE_RANK_1', '1.50', N'Base odds for rank 1 horse'),
('ODDS_BASE_RANK_2', '1.65', N'Base odds for rank 2 horse'),
('ODDS_BASE_RANK_3', '1.80', N'Base odds for rank 3 horse'),
('ODDS_BASE_RANK_4', '2.00', N'Base odds for rank 4 horse'),
('ODDS_BASE_RANK_5', '2.20', N'Base odds for rank 5 horse'),
('ODDS_BASE_RANK_6', '2.40', N'Base odds for rank 6 horse'),
('ODDS_BASE_RANK_7', '2.60', N'Base odds for rank 7 horse'),
('ODDS_BASE_RANK_8', '2.80', N'Base odds for rank 8 horse'),
('ODDS_BASE_RANK_9', '3.00', N'Base odds for rank 9 horse'),
('ODDS_BASE_RANK_10', '3.25', N'Base odds for rank 10 horse'),
('ODDS_BASE_RANK_11', '3.50', N'Base odds for rank 11 horse'),
('ODDS_BASE_RANK_12', '3.75', N'Base odds for rank 12 horse'),
('ODDS_BASE_RANK_13', '4.00', N'Base odds for rank 13 horse'),
('ODDS_BASE_RANK_14', '4.25', N'Base odds for rank 14 horse'),
('ODDS_BASE_RANK_15', '4.50', N'Base odds for rank 15 horse'),
('ODDS_BASE_RANK_OVER_15', '5.00', N'Base odds for horses outside top 15'),
('ODDS_BASE_UNRANKED', '2.50', N'Base odds for unranked horses'),
('EXACT_POSITION_FACTOR', '0.75', N'Odds increase factor for exact position bets'),
('ODDS_MAX', '15.00', N'Maximum odds limit')
) AS src(ConfigKey, ConfigValue, Description)
ON target.ConfigKey = src.ConfigKey
WHEN NOT MATCHED THEN
    INSERT (ConfigKey, ConfigValue, Description) VALUES (src.ConfigKey, src.ConfigValue, src.Description);

DECLARE @AdminUserID INT = (SELECT UserID FROM Users WHERE Username = 'admin');
DECLARE @OrganizerUserID INT = (SELECT UserID FROM Users WHERE Username = 'organizer1');
DECLARE @Owner1UserID INT = (SELECT UserID FROM Users WHERE Username = 'owner1');
DECLARE @Owner2UserID INT = (SELECT UserID FROM Users WHERE Username = 'owner2');
DECLARE @Jockey1UserID INT = (SELECT UserID FROM Users WHERE Username = 'jockey1');
DECLARE @Jockey2UserID INT = (SELECT UserID FROM Users WHERE Username = 'jockey2');
DECLARE @RefereeUserID INT = (SELECT UserID FROM Users WHERE Username = 'referee1');
DECLARE @SpectatorUserID INT = (SELECT UserID FROM Users WHERE Username = 'spectator1');

DECLARE @Owner1ID INT = (SELECT OwnerID FROM HorseOwners WHERE UserID = @Owner1UserID);
DECLARE @Owner2ID INT = (SELECT OwnerID FROM HorseOwners WHERE UserID = @Owner2UserID);
DECLARE @Jockey1ID INT = (SELECT JockeyID FROM Jockeys WHERE UserID = @Jockey1UserID);
DECLARE @Jockey2ID INT = (SELECT JockeyID FROM Jockeys WHERE UserID = @Jockey2UserID);
DECLARE @RefereeID INT = (SELECT RefereeID FROM Referees WHERE UserID = @RefereeUserID);

-- 2) Admin flow: one clean pending account for approve/reject tests.
IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'flow_pending_owner')
BEGIN
    INSERT INTO Users (Username, PasswordHash, FullName, Email, Phone, RoleID, IsActive, IsApproved)
    VALUES ('flow_pending_owner', @Pwd, N'FLOW Admin Pending Owner', 'flow.pending.owner@gmail.com', '0900000111', 2, 1, 0);
END;

DECLARE @FlowPendingOwnerUserID INT = (SELECT UserID FROM Users WHERE Username = 'flow_pending_owner');
IF @FlowPendingOwnerUserID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM HorseOwners WHERE UserID = @FlowPendingOwnerUserID)
BEGIN
    INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber)
    VALUES (@FlowPendingOwnerUserID, '079205009999', N'Ha Noi', N'FLOW Pending Stable', 'FLOW-OWN-PENDING');
END;

-- 3) Organizer/Admin tournament flow: Draft, PendingApproval, and Open states.
IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'FLOW_ADMIN_PENDING_TOURNAMENT')
BEGIN
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy)
    VALUES (N'FLOW_ADMIN_PENDING_TOURNAMENT', N'Tournament waiting for Admin approval.', N'Ha Noi', DATEADD(DAY, 7, CAST(GETDATE() AS DATE)), DATEADD(DAY, 14, CAST(GETDATE() AS DATE)), 30000000, 12, 12, 'PendingApproval', @OrganizerUserID);
END;

IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'FLOW_ORGANIZER_DRAFT_TOURNAMENT')
BEGIN
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy)
    VALUES (N'FLOW_ORGANIZER_DRAFT_TOURNAMENT', N'Tournament draft for Organizer editing and Admin submission.', N'Ho Chi Minh City', DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), DATEADD(DAY, 18, CAST(GETDATE() AS DATE)), 45000000, 16, 16, 'Draft', @OrganizerUserID);
END;

IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'FLOW_TEST_OPEN_TOURNAMENT')
BEGIN
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt)
    VALUES (N'FLOW_TEST_OPEN_TOURNAMENT', N'Open tournament for Owner, Referee, Spectator, and Organizer tests.', N'Da Nang', DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 20, CAST(GETDATE() AS DATE)), 60000000, 24, 24, 'Open', @OrganizerUserID, @AdminUserID, GETDATE());
END;

DECLARE @FlowTournamentID INT = (SELECT TournamentID FROM Tournaments WHERE TournamentName = N'FLOW_TEST_OPEN_TOURNAMENT');

IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @FlowTournamentID AND RoundName = N'FLOW Round 1')
BEGIN
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@FlowTournamentID, N'FLOW Round 1', 1, CAST(GETDATE() AS DATE), DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), N'Round used to test all 5 flows.');
END;

DECLARE @FlowRoundID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @FlowTournamentID AND RoundName = N'FLOW Round 1');

-- 4) Add isolated test horses without touching base seed data.
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H001')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner1ID, N'FLOW Owner Horse Ready', N'Thoroughbred', 2020, N'Black', N'Male', 450.00, 'FLOW-H001', N'Active', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H002')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner1ID, N'FLOW Owner Horse Invite', N'Arabian', 2021, N'Brown', N'Female', 430.00, 'FLOW-H002', N'Active', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H003')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner2ID, N'FLOW Pending Approval Horse', N'Mustang', 2020, N'White', N'Male', 440.00, 'FLOW-H003', N'Active', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H004')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner2ID, N'FLOW Betting Horse A', N'Warmblood', 2019, N'Gray', N'Male', 455.00, 'FLOW-H004', N'Active', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H005')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner1ID, N'FLOW Betting Horse B', N'Andalusian', 2018, N'Golden', N'Female', 420.00, 'FLOW-H005', N'Active', 1);
END;

DECLARE @HorseReady INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H001');
DECLARE @HorseInvite INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H002');
DECLARE @HorsePending INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H003');
DECLARE @HorseBetA INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H004');
DECLARE @HorseBetB INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H005');

IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @HorseReady)
BEGIN
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@HorseReady, CAST(GETDATE() AS DATE), N'FLOW Vet', N'Active', N'Eligible to race', N'Demo pre-race check', @OrganizerUserID);
END;
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @HorseBetA)
BEGIN
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@HorseBetA, CAST(GETDATE() AS DATE), N'FLOW Vet', N'Active', N'Eligible to race', N'Demo pre-race check', @OrganizerUserID);
END;
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @HorsePending)
BEGIN
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@HorsePending, CAST(GETDATE() AS DATE), N'FLOW Vet', N'Active', N'Eligible for Organizer entry approval', N'Demo entry approval', @OrganizerUserID);
END;

-- 5) Separate races per flow so demos do not affect each other.
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'FLOW_OWNER_ENTRY_RACE')
BEGIN
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@FlowTournamentID, @FlowRoundID, N'FLOW_OWNER_ENTRY_RACE', DATEADD(DAY, 4, GETDATE()), 1000, N'Flat', 8, 10000000, 5000000, 2000000, 'RegistrationOpen', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 3, GETDATE()));
END;
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'FLOW_REFEREE_RESULT_RACE')
BEGIN
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@FlowTournamentID, @FlowRoundID, N'FLOW_REFEREE_RESULT_RACE', DATEADD(DAY, 2, GETDATE()), 1200, N'Flat', 8, 15000000, 7000000, 3000000, 'RegistrationOpen', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()));
END;
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'FLOW_BETTING_RACE')
BEGIN
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@FlowTournamentID, @FlowRoundID, N'FLOW_BETTING_RACE', DATEADD(DAY, 6, GETDATE()), 900, N'Flat', 8, 12000000, 6000000, 3000000, 'RegistrationOpen', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 5, GETDATE()));
END;
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'FLOW_ORGANIZER_APPROVAL_RACE')
BEGIN
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@FlowTournamentID, @FlowRoundID, N'FLOW_ORGANIZER_APPROVAL_RACE', DATEADD(DAY, 7, GETDATE()), 1100, N'Flat', 8, 13000000, 6000000, 2500000, 'RegistrationOpen', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 6, GETDATE()));
END;

DECLARE @OwnerRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'FLOW_OWNER_ENTRY_RACE');
DECLARE @RefRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'FLOW_REFEREE_RESULT_RACE');
DECLARE @BetRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'FLOW_BETTING_RACE');
DECLARE @OrgRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'FLOW_ORGANIZER_APPROVAL_RACE');

-- 6) Entries for each flow.
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @OwnerRaceID AND HorseID = @HorseInvite)
BEGIN
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@OwnerRaceID, @HorseInvite, NULL, 1, 'Approved', 1, @OrganizerUserID, 0, 2.20);
END;
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefRaceID AND HorseID = @HorseReady)
BEGIN
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefRaceID, @HorseReady, @Jockey1ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.50);
END;
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefRaceID AND HorseID = @HorseBetA)
BEGIN
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefRaceID, @HorseBetA, @Jockey2ID, 2, 'Ready', 1, @OrganizerUserID, 1, 1.80);
END;
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @BetRaceID AND HorseID = @HorseBetA)
BEGIN
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@BetRaceID, @HorseBetA, @Jockey1ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.65);
END;
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @BetRaceID AND HorseID = @HorseBetB)
BEGIN
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@BetRaceID, @HorseBetB, @Jockey2ID, 2, 'Ready', 1, @OrganizerUserID, 1, 2.00);
END;
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @OrgRaceID AND HorseID = @HorsePending)
BEGIN
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@OrgRaceID, @HorsePending, NULL, 3, 'Pending', 0, NULL, 0, 2.50);
END;

DECLARE @OwnerEntryID INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @OwnerRaceID AND HorseID = @HorseInvite);
DECLARE @RefEntry1ID INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @RefRaceID AND HorseID = @HorseReady);
DECLARE @BetEntry1ID INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @BetRaceID AND HorseID = @HorseBetA);
DECLARE @OrgPendingEntryID INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @OrgRaceID AND HorseID = @HorsePending);

-- 7) Owner/Jockey flow: entry approved without jockey and has pending invitation.
IF NOT EXISTS (SELECT 1 FROM JockeyInvitations WHERE EntryID = @OwnerEntryID AND JockeyID = @Jockey2ID)
BEGIN
    INSERT INTO JockeyInvitations (EntryID, JockeyID, InvitedByOwner, Message, Status)
    VALUES (@OwnerEntryID, @Jockey2ID, @Owner1ID, N'FLOW: inviting jockey2 to ride in Owner Entry Race.', 'Pending');
END;

-- 8) Referee assignment and notification for the isolated race.
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @RefRaceID AND RefereeID = @RefereeID)
BEGIN
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@RefRaceID, @RefereeID, N'Chief');
END;
IF NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefRaceID)
BEGIN
    INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
    VALUES (@RefereeUserID, N'FLOW: Referee assignment received', N'You are the chief referee for FLOW_REFEREE_RESULT_RACE.', 'RefereeAssigned', @RefRaceID, 'Race');
END;

-- 9) Betting flow: preloaded wallet and one Pending bet for history testing.
IF EXISTS (SELECT 1 FROM Wallets WHERE UserID = @SpectatorUserID)
BEGIN
    UPDATE Wallets SET Balance = CASE WHEN Balance < 2000000 THEN 2000000 ELSE Balance END, UpdatedAt = GETDATE()
    WHERE UserID = @SpectatorUserID;
END;
ELSE
BEGIN
    INSERT INTO Wallets (UserID, Balance) VALUES (@SpectatorUserID, 2000000);
END;

IF NOT EXISTS (SELECT 1 FROM Bets WHERE UserID = @SpectatorUserID AND RaceID = @BetRaceID AND EntryID = @BetEntry1ID AND BetType = 'WIN')
BEGIN
    INSERT INTO Bets (UserID, RaceID, EntryID, BetType, Amount, Odds, PotentialPayout, Status)
    VALUES (@SpectatorUserID, @BetRaceID, @BetEntry1ID, 'WIN', 50000, 1.65, 82500, 'Pending');

    DECLARE @FlowSeedBetID INT = SCOPE_IDENTITY();
    INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
    SELECT WalletID, -50000, 'BetPlaced', N'FLOW seed: WIN bet for FLOW_BETTING_RACE', 'Bet', @FlowSeedBetID
    FROM Wallets WHERE UserID = @SpectatorUserID;
END;

-- 10) Organizer flow: pending entry, assigned referee list, and notification sample.
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @OrgRaceID AND RefereeID = @RefereeID)
BEGIN
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@OrgRaceID, @RefereeID, N'Assistant');
END;
IF NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @OrganizerUserID AND NotifType = 'EntryPendingApproval' AND RelatedEntityID = @OrgPendingEntryID)
BEGIN
    INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
    VALUES (@OrganizerUserID, N'FLOW: Entry pending approval', N'FLOW Pending Approval Horse is waiting for Organizer approval.', 'EntryPendingApproval', @OrgPendingEntryID, 'Entry');
END;

-- 11) Leaderboard/stats data for rankings and odds calculations.
MERGE HorseTournamentStats AS target
USING (VALUES
(@FlowTournamentID, @HorseReady, 3, 2, 3, 45000000, 30),
(@FlowTournamentID, @HorseBetA, 2, 1, 2, 22000000, 20),
(@FlowTournamentID, @HorseBetB, 2, 0, 1, 6000000, 10),
(@FlowTournamentID, @HorseInvite, 1, 0, 0, 0, 3)
) AS src(TournamentID, HorseID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
ON target.TournamentID = src.TournamentID AND target.HorseID = src.HorseID
WHEN NOT MATCHED THEN
    INSERT (TournamentID, HorseID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
    VALUES (src.TournamentID, src.HorseID, src.TotalRaces, src.TotalWins, src.TotalPodiums, src.TotalPrize, src.Points);

MERGE JockeyTournamentStats AS target
USING (VALUES
(@FlowTournamentID, @Jockey1ID, 3, 2, 3, 45000000, 30),
(@FlowTournamentID, @Jockey2ID, 2, 0, 2, 12000000, 15)
) AS src(TournamentID, JockeyID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
ON target.TournamentID = src.TournamentID AND target.JockeyID = src.JockeyID
WHEN NOT MATCHED THEN
    INSERT (TournamentID, JockeyID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
    VALUES (src.TournamentID, src.JockeyID, src.TotalRaces, src.TotalWins, src.TotalPodiums, src.TotalPrize, src.Points);

-- 12) Extra demo seed: users, horses, tournaments, races, bets, and results for richer UI demos.
IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'organizer2')
BEGIN
    INSERT INTO Users (Username, PasswordHash, FullName, Email, Phone, RoleID, IsActive, IsApproved) VALUES
    ('organizer2', @Pwd, N'Organizer B', 'organizer2@gmail.com', '0900000210', 6, 1, 1),
    ('owner3', @Pwd, N'Owner C', 'owner3@gmail.com', '0900000211', 2, 1, 1),
    ('owner4', @Pwd, N'Owner D', 'owner4@gmail.com', '0900000212', 2, 1, 1),
    ('jockey3', @Pwd, N'Jockey E', 'jockey3@gmail.com', '0900000213', 3, 1, 1),
    ('jockey4', @Pwd, N'Jockey F', 'jockey4@gmail.com', '0900000214', 3, 1, 1),
    ('referee2', @Pwd, N'Referee F', 'referee2@gmail.com', '0900000215', 4, 1, 1),
    ('spectator2', @Pwd, N'Spectator B', 'spectator2@gmail.com', '0900000216', 5, 1, 1),
    ('spectator3', @Pwd, N'Spectator C', 'spectator3@gmail.com', '0900000217', 5, 1, 1);
END;

DECLARE @Organizer2UserID INT = (SELECT UserID FROM Users WHERE Username = 'organizer2');
DECLARE @Owner3UserID INT = (SELECT UserID FROM Users WHERE Username = 'owner3');
DECLARE @Owner4UserID INT = (SELECT UserID FROM Users WHERE Username = 'owner4');
DECLARE @Jockey3UserID INT = (SELECT UserID FROM Users WHERE Username = 'jockey3');
DECLARE @Jockey4UserID INT = (SELECT UserID FROM Users WHERE Username = 'jockey4');
DECLARE @Referee2UserID INT = (SELECT UserID FROM Users WHERE Username = 'referee2');
DECLARE @Spectator2UserID INT = (SELECT UserID FROM Users WHERE Username = 'spectator2');
DECLARE @Spectator3UserID INT = (SELECT UserID FROM Users WHERE Username = 'spectator3');

IF NOT EXISTS (SELECT 1 FROM HorseOwners WHERE UserID = @Owner3UserID)
    INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber)
    VALUES (@Owner3UserID, '079205002011', N'Can Tho', N'Mekong Stable', 'OWN003');
IF NOT EXISTS (SELECT 1 FROM HorseOwners WHERE UserID = @Owner4UserID)
    INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber)
    VALUES (@Owner4UserID, '079205002012', N'Hai Phong', N'Ocean Stable', 'OWN004');
IF NOT EXISTS (SELECT 1 FROM Jockeys WHERE UserID = @Jockey3UserID)
    INSERT INTO Jockeys (UserID, NationalID, LicenseNumber, WeightKg, HeightCm, ExperienceYear)
    VALUES (@Jockey3UserID, '079205002013', 'JK003', 54.00, 169.00, 3);
IF NOT EXISTS (SELECT 1 FROM Jockeys WHERE UserID = @Jockey4UserID)
    INSERT INTO Jockeys (UserID, NationalID, LicenseNumber, WeightKg, HeightCm, ExperienceYear)
    VALUES (@Jockey4UserID, '079205002014', 'JK004', 52.50, 166.00, 2);
IF NOT EXISTS (SELECT 1 FROM Referees WHERE UserID = @Referee2UserID)
    INSERT INTO Referees (UserID, BadgeNumber, Speciality)
    VALUES (@Referee2UserID, 'REF002', N'Assistant Referee');

INSERT INTO Wallets (UserID, Balance)
SELECT UserID,
       CASE WHEN RoleID = 5 THEN 2500000 WHEN RoleID = 2 THEN 1200000 ELSE 0 END
FROM Users
WHERE Username IN ('organizer2','owner3','owner4','jockey3','jockey4','referee2','spectator2','spectator3')
  AND NOT EXISTS (SELECT 1 FROM Wallets w WHERE w.UserID = Users.UserID);

DECLARE @Owner3ID INT = (SELECT OwnerID FROM HorseOwners WHERE UserID = @Owner3UserID);
DECLARE @Owner4ID INT = (SELECT OwnerID FROM HorseOwners WHERE UserID = @Owner4UserID);
DECLARE @Jockey3ID INT = (SELECT JockeyID FROM Jockeys WHERE UserID = @Jockey3UserID);
DECLARE @Jockey4ID INT = (SELECT JockeyID FROM Jockeys WHERE UserID = @Jockey4UserID);
DECLARE @Referee2ID INT = (SELECT RefereeID FROM Referees WHERE UserID = @Referee2UserID);

IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H001')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner3ID, N'Sao Bang', N'Thoroughbred', 2020, N'Black White', N'Male', 448.00, 'DM-H001', N'Active', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H002')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner3ID, N'Ngan Ha', N'Arabian', 2021, N'Silver Gray', N'Female', 426.00, 'DM-H002', N'Active', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H003')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner4ID, N'Phong Van', N'Mustang', 2019, N'Dark Brown', N'Male', 452.00, 'DM-H003', N'Active', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H004')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner4ID, N'Lam Son', N'Warmblood', 2018, N'Golden Brown', N'Male', 463.00, 'DM-H004', N'Active', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H005')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner3ID, N'Hong Nhat', N'Andalusian', 2020, N'Reddish Brown', N'Female', 432.00, 'DM-H005', N'Injured', @OrganizerUserID, GETDATE(), 1);

DECLARE @Dmh1 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H001');
DECLARE @Dmh2 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H002');
DECLARE @Dmh3 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H003');
DECLARE @Dmh4 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H004');
DECLARE @Dmh5 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H005');

IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @Dmh1)
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@Dmh1, CAST(GETDATE() AS DATE), N'Dr. Nguyen Minh', N'Active', N'Eligible to race', N'Extended demo seed', @OrganizerUserID);
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @Dmh3)
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@Dmh3, CAST(GETDATE() AS DATE), N'Dr. Nguyen Minh', N'Active', N'Eligible to race', N'Extended demo seed', @OrganizerUserID);
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @Dmh5)
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@Dmh5, CAST(GETDATE() AS DATE), N'Dr. Nguyen Minh', N'Injured', N'Needs additional monitoring before approval', N'Demo horse health status data', @OrganizerUserID);

IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'DEMO_SHOWCASE_CUP_2026')
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt)
    VALUES (N'DEMO_SHOWCASE_CUP_2026', N'Showcase tournament with multiple users and horses for UI demos.', N'Ha Noi', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 12, CAST(GETDATE() AS DATE)), 80000000, 32, 32, 'Open', @OrganizerUserID, @AdminUserID, GETDATE());

DECLARE @DemoTournamentID INT = (SELECT TournamentID FROM Tournaments WHERE TournamentName = N'DEMO_SHOWCASE_CUP_2026');
IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Qualifying Round')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@DemoTournamentID, N'DEMO Qualifying Round', 1, DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 5, CAST(GETDATE() AS DATE)), N'Showcase qualifying round');
IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Final Round')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@DemoTournamentID, N'DEMO Final Round', 2, DATEADD(DAY, 8, CAST(GETDATE() AS DATE)), DATEADD(DAY, 12, CAST(GETDATE() AS DATE)), N'Showcase final round');

DECLARE @DemoRound1ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Qualifying Round');
DECLARE @DemoRound2ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Final Round');

IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'DEMO_SHOWCASE_OPEN_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@DemoTournamentID, @DemoRound1ID, N'DEMO_SHOWCASE_OPEN_RACE', DATEADD(DAY, 3, GETDATE()), 1000, N'Flat', 10, 18000000, 9000000, 4000000, 'RegistrationOpen', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 2, GETDATE()));
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'DEMO_SHOWCASE_REFEREE_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@DemoTournamentID, @DemoRound1ID, N'DEMO_SHOWCASE_REFEREE_RACE', DATEADD(DAY, 2, GETDATE()), 1300, N'Flat', 10, 22000000, 11000000, 5000000, 'RegistrationOpen', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()));
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'DEMO_SHOWCASE_FINISHED_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@DemoTournamentID, @DemoRound2ID, N'DEMO_SHOWCASE_FINISHED_RACE', DATEADD(DAY, -2, GETDATE()), 1500, N'Flat', 10, 30000000, 16000000, 7000000, 'Finished', DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, -4, GETDATE()));

DECLARE @DemoOpenRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'DEMO_SHOWCASE_OPEN_RACE');
DECLARE @DemoRefRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'DEMO_SHOWCASE_REFEREE_RACE');
DECLARE @DemoFinishedRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'DEMO_SHOWCASE_FINISHED_RACE');

IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoOpenRaceID AND HorseID = @Dmh1)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoOpenRaceID, @Dmh1, @Jockey3ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.70);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoOpenRaceID AND HorseID = @Dmh2)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoOpenRaceID, @Dmh2, NULL, 2, 'Approved', 1, @OrganizerUserID, 0, 2.10);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoOpenRaceID AND HorseID = @Dmh5)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoOpenRaceID, @Dmh5, NULL, 5, 'Pending', 0, NULL, 0, 2.80);

IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoRefRaceID AND HorseID = @Dmh1)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoRefRaceID, @Dmh1, @Jockey3ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.60);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoRefRaceID AND HorseID = @Dmh3)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoRefRaceID, @Dmh3, @Jockey4ID, 2, 'Ready', 1, @OrganizerUserID, 1, 1.95);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoRefRaceID AND HorseID = @Dmh4)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoRefRaceID, @Dmh4, @Jockey1ID, 3, 'Ready', 1, @OrganizerUserID, 1, 2.20);

IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoFinishedRaceID AND HorseID = @Dmh1)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoFinishedRaceID, @Dmh1, @Jockey3ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.55);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoFinishedRaceID AND HorseID = @Dmh3)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoFinishedRaceID, @Dmh3, @Jockey4ID, 2, 'Ready', 1, @OrganizerUserID, 1, 1.85);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @DemoFinishedRaceID AND HorseID = @HorseReady)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@DemoFinishedRaceID, @HorseReady, @Jockey1ID, 3, 'Ready', 1, @OrganizerUserID, 1, 2.05);

DECLARE @DemoOpenEntry1 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoOpenRaceID AND HorseID = @Dmh1);
DECLARE @DemoOpenEntry2 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoOpenRaceID AND HorseID = @Dmh2);
DECLARE @DemoOpenPendingEntry INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoOpenRaceID AND HorseID = @Dmh5);
DECLARE @DemoRefEntry1 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoRefRaceID AND HorseID = @Dmh1);
DECLARE @DemoRefEntry2 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoRefRaceID AND HorseID = @Dmh3);
DECLARE @DemoRefEntry3 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoRefRaceID AND HorseID = @Dmh4);
DECLARE @DemoFinalEntry1 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoFinishedRaceID AND HorseID = @Dmh1);
DECLARE @DemoFinalEntry2 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoFinishedRaceID AND HorseID = @Dmh3);
DECLARE @DemoFinalEntry3 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @DemoFinishedRaceID AND HorseID = @HorseReady);

IF NOT EXISTS (SELECT 1 FROM JockeyInvitations WHERE EntryID = @DemoOpenEntry2 AND JockeyID = @Jockey4ID)
    INSERT INTO JockeyInvitations (EntryID, JockeyID, InvitedByOwner, Message, Status)
    VALUES (@DemoOpenEntry2, @Jockey4ID, @Owner3ID, N'DEMO: inviting jockey4 to ride Ngan Ha.', 'Pending');

IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @DemoRefRaceID AND RefereeID = @RefereeID)
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@DemoRefRaceID, @RefereeID, N'Chief');
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @DemoRefRaceID AND RefereeID = @Referee2ID)
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@DemoRefRaceID, @Referee2ID, N'Assistant');
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @DemoFinishedRaceID AND RefereeID = @Referee2ID)
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@DemoFinishedRaceID, @Referee2ID, N'Chief');

IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry1)
    INSERT INTO RaceResults (RaceID, EntryID, FinishTime, FinishPosition, DNF, DQ, ConfirmedByRef, ConfirmedAt, ApprovalStatus, ApprovedByOrganizer, ApprovedAt, PublishedAt)
    VALUES (@DemoFinishedRaceID, @DemoFinalEntry1, 92.400, 1, 0, 0, @Referee2ID, GETDATE(), 'Published', @OrganizerUserID, GETDATE(), GETDATE());
IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry2)
    INSERT INTO RaceResults (RaceID, EntryID, FinishTime, FinishPosition, DNF, DQ, ConfirmedByRef, ConfirmedAt, ApprovalStatus, ApprovedByOrganizer, ApprovedAt, PublishedAt)
    VALUES (@DemoFinishedRaceID, @DemoFinalEntry2, 90.800, 2, 0, 0, @Referee2ID, GETDATE(), 'Published', @OrganizerUserID, GETDATE(), GETDATE());
IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry3)
    INSERT INTO RaceResults (RaceID, EntryID, FinishTime, FinishPosition, DNF, DQ, ConfirmedByRef, ConfirmedAt, ApprovalStatus, ApprovedByOrganizer, ApprovedAt, PublishedAt)
    VALUES (@DemoFinishedRaceID, @DemoFinalEntry3, 94.100, 3, 0, 0, @Referee2ID, GETDATE(), 'Published', @OrganizerUserID, GETDATE(), GETDATE());

IF NOT EXISTS (SELECT 1 FROM Violations WHERE RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry2 AND ViolationType = 'LanLane')
    INSERT INTO Violations (RaceID, EntryID, RefereeID, ViolationType, PenaltySeconds, IsDQ, EvidenceImageURL, Description)
    VALUES (@DemoFinishedRaceID, @DemoFinalEntry2, @Referee2ID, 'LanLane', 5.00, 0, N'demo-evidence/lan-lane.jpg', N'Lane violation near the final corner, plus 5 seconds.');
IF NOT EXISTS (SELECT 1 FROM Violations WHERE RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry3 AND ViolationType = 'CanDuong')
    INSERT INTO Violations (RaceID, EntryID, RefereeID, ViolationType, PenaltySeconds, IsDQ, EvidenceImageURL, Description)
    VALUES (@DemoFinishedRaceID, @DemoFinalEntry3, @Referee2ID, 'CanDuong', 10.00, 0, N'demo-evidence/can-duong.jpg', N'Obstructed another competitor, plus 10 seconds.');

EXEC sp_UpdateRaceResultRanking @DemoFinishedRaceID;

IF NOT EXISTS (SELECT 1 FROM RaceMinutes WHERE RaceID = @DemoFinishedRaceID)
    INSERT INTO RaceMinutes (RaceID, RefereeID, Content, WeatherCondition, PreRaceChecks, PostRaceNotes, MinutesFileURL, SentToOwners, SentAt)
    VALUES (@DemoFinishedRaceID, @Referee2ID, N'Demo race minutes were created and sent to owners.', N'Clear weather', N'Horses, jockeys, and lanes were checked before the race.', N'Two violations were recorded.', N'demo-uploads/showcase-minutes.webp', 1, GETDATE());

IF NOT EXISTS (SELECT 1 FROM Bets WHERE UserID = @Spectator2UserID AND RaceID = @DemoOpenRaceID AND EntryID = @DemoOpenEntry1 AND BetType = 'WIN')
BEGIN
    INSERT INTO Bets (UserID, RaceID, EntryID, BetType, Amount, Odds, PotentialPayout, Status)
    VALUES (@Spectator2UserID, @DemoOpenRaceID, @DemoOpenEntry1, 'WIN', 100000, 1.70, 170000, 'Pending');
    DECLARE @DemoBet1 INT = SCOPE_IDENTITY();
    INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
    SELECT WalletID, -100000, 'BetPlaced', N'DEMO: WIN bet placed on Sao Bang', 'Bet', @DemoBet1
    FROM Wallets WHERE UserID = @Spectator2UserID;
END;
IF NOT EXISTS (SELECT 1 FROM Bets WHERE UserID = @Spectator3UserID AND RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry1 AND BetType = 'WIN')
BEGIN
    INSERT INTO Bets (UserID, RaceID, EntryID, BetType, Amount, Odds, PotentialPayout, Status, SettledAt)
    VALUES (@Spectator3UserID, @DemoFinishedRaceID, @DemoFinalEntry1, 'WIN', 80000, 1.55, 124000, 'Won', GETDATE());
    DECLARE @DemoBet2 INT = SCOPE_IDENTITY();
    INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
    SELECT WalletID, 124000, 'BetWon', N'DEMO: winning payout for finished race', 'Bet', @DemoBet2
    FROM Wallets WHERE UserID = @Spectator3UserID;
END;

INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @Referee2UserID, N'DEMO: Referee assignment received', N'You are the chief referee for DEMO_SHOWCASE_FINISHED_RACE.', 'RefereeAssigned', @DemoFinishedRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @Referee2UserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @DemoFinishedRaceID);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @OrganizerUserID, N'DEMO: Entry requires health review', N'Hong Nhat is waiting for approval and its health status needs review.', 'EntryPendingApproval', @DemoOpenPendingEntry, 'Entry'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @OrganizerUserID AND NotifType = 'EntryPendingApproval' AND RelatedEntityID = @DemoOpenPendingEntry);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @Owner3UserID, N'DEMO: Race minutes sent', N'Race minutes for DEMO_SHOWCASE_FINISHED_RACE were sent to the owner.', 'RaceMinutesSent', @DemoFinishedRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @Owner3UserID AND NotifType = 'RaceMinutesSent' AND RelatedEntityID = @DemoFinishedRaceID);

MERGE HorseTournamentStats AS target
USING (VALUES
(@DemoTournamentID, @Dmh1, 5, 2, 4, 48000000, 38),
(@DemoTournamentID, @Dmh2, 3, 1, 2, 18000000, 19),
(@DemoTournamentID, @Dmh3, 4, 1, 3, 32000000, 28),
(@DemoTournamentID, @Dmh4, 2, 0, 1, 7000000, 9),
(@DemoTournamentID, @Dmh5, 1, 0, 0, 0, 1)
) AS src(TournamentID, HorseID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
ON target.TournamentID = src.TournamentID AND target.HorseID = src.HorseID
WHEN NOT MATCHED THEN
    INSERT (TournamentID, HorseID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
    VALUES (src.TournamentID, src.HorseID, src.TotalRaces, src.TotalWins, src.TotalPodiums, src.TotalPrize, src.Points);

MERGE JockeyTournamentStats AS target
USING (VALUES
(@DemoTournamentID, @Jockey3ID, 5, 2, 4, 48000000, 38),
(@DemoTournamentID, @Jockey4ID, 4, 1, 3, 32000000, 28),
(@DemoTournamentID, @Jockey1ID, 3, 0, 2, 15000000, 15)
) AS src(TournamentID, JockeyID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
ON target.TournamentID = src.TournamentID AND target.JockeyID = src.JockeyID
WHEN NOT MATCHED THEN
    INSERT (TournamentID, JockeyID, TotalRaces, TotalWins, TotalPodiums, TotalPrize, Points)
    VALUES (src.TournamentID, src.JockeyID, src.TotalRaces, src.TotalWins, src.TotalPodiums, src.TotalPrize, src.Points);

-- 13) Referee1 demo pack: isolated data for a full referee review flow.
IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'REFEREE1_DEMO_CUP_2026')
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt)
    VALUES (N'REFEREE1_DEMO_CUP_2026',
            N'Private demo tournament for referee1: receive assignment, pre-race check, enter results, record violations, and create race minutes.',
            N'Ho Chi Minh City',
            DATEADD(DAY, -2, CAST(GETDATE() AS DATE)),
            DATEADD(DAY, 10, CAST(GETDATE() AS DATE)),
            90000000, 30, 30, 'Open', @OrganizerUserID, @AdminUserID, GETDATE());

DECLARE @RefDemoTournamentID INT = (SELECT TournamentID FROM Tournaments WHERE TournamentName = N'REFEREE1_DEMO_CUP_2026');

IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Qualifying Round')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@RefDemoTournamentID, N'Referee1 Qualifying Round', 1, DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 4, CAST(GETDATE() AS DATE)), N'Round for referee1 pre-race checks and data entry.');
IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Final Round')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@RefDemoTournamentID, N'Referee1 Final Round', 2, DATEADD(DAY, 5, CAST(GETDATE() AS DATE)), DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), N'Round for referee1 to review finished race results and minutes.');

DECLARE @RefDemoRound1ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Qualifying Round');
DECLARE @RefDemoRound2ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Final Round');

IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'REF1_PRECHECK_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@RefDemoTournamentID, @RefDemoRound1ID, N'REF1_PRECHECK_RACE', DATEADD(DAY, 1, GETDATE()), 1000, N'Flat', 8, 15000000, 8000000, 3000000, 'RegistrationOpen', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()));
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'REF1_ONGOING_INPUT_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@RefDemoTournamentID, @RefDemoRound1ID, N'REF1_ONGOING_INPUT_RACE', DATEADD(HOUR, 2, GETDATE()), 1200, N'Flat', 8, 20000000, 10000000, 5000000, 'Ongoing', DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -2, GETDATE()));
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'REF1_FINISHED_MINUTES_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@RefDemoTournamentID, @RefDemoRound2ID, N'REF1_FINISHED_MINUTES_RACE', DATEADD(DAY, -1, GETDATE()), 1400, N'Flat', 8, 25000000, 12000000, 6000000, 'Finished', DATEADD(DAY, -8, GETDATE()), DATEADD(DAY, -3, GETDATE()));

DECLARE @RefPreRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'REF1_PRECHECK_RACE');
DECLARE @RefOngoingRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'REF1_ONGOING_INPUT_RACE');
DECLARE @RefFinishedRaceID INT = (SELECT RaceID FROM Races WHERE RaceName = N'REF1_FINISHED_MINUTES_RACE');

IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @RefPreRaceID AND RefereeID = @RefereeID)
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@RefPreRaceID, @RefereeID, N'Chief');
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @RefOngoingRaceID AND RefereeID = @RefereeID)
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@RefOngoingRaceID, @RefereeID, N'Chief');
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @RefFinishedRaceID AND RefereeID = @RefereeID)
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@RefFinishedRaceID, @RefereeID, N'Chief');
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @RefFinishedRaceID AND RefereeID = @Referee2ID)
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@RefFinishedRaceID, @Referee2ID, N'Assistant');

IF NOT EXISTS (SELECT 1 FROM RaceStatusHistory WHERE RaceID = @RefOngoingRaceID AND NewStatus = 'Ongoing')
    INSERT INTO RaceStatusHistory (RaceID, OldStatus, NewStatus, ChangedBy)
    VALUES (@RefOngoingRaceID, 'RegistrationOpen', 'Ongoing', @RefereeUserID);
IF NOT EXISTS (SELECT 1 FROM RaceStatusHistory WHERE RaceID = @RefFinishedRaceID AND NewStatus = 'Finished')
    INSERT INTO RaceStatusHistory (RaceID, OldStatus, NewStatus, ChangedBy)
    VALUES (@RefFinishedRaceID, 'Ongoing', 'Finished', @RefereeUserID);

IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefPreRaceID AND HorseID = @HorseReady)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefPreRaceID, @HorseReady, @Jockey1ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.55);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefPreRaceID AND HorseID = @Dmh1)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefPreRaceID, @Dmh1, @Jockey3ID, 2, 'Ready', 1, @OrganizerUserID, 1, 1.80);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefPreRaceID AND HorseID = @Dmh3)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefPreRaceID, @Dmh3, @Jockey4ID, 3, 'Ready', 1, @OrganizerUserID, 1, 2.10);

IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefOngoingRaceID AND HorseID = @HorseBetA)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefOngoingRaceID, @HorseBetA, @Jockey2ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.70);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefOngoingRaceID AND HorseID = @Dmh2)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefOngoingRaceID, @Dmh2, @Jockey4ID, 2, 'Ready', 1, @OrganizerUserID, 1, 2.05);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefOngoingRaceID AND HorseID = @Dmh4)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefOngoingRaceID, @Dmh4, @Jockey1ID, 3, 'Ready', 1, @OrganizerUserID, 1, 2.30);

IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefFinishedRaceID AND HorseID = @HorseBetB)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefFinishedRaceID, @HorseBetB, @Jockey2ID, 1, 'Ready', 1, @OrganizerUserID, 1, 1.65);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefFinishedRaceID AND HorseID = @Dmh1)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefFinishedRaceID, @Dmh1, @Jockey3ID, 2, 'Ready', 1, @OrganizerUserID, 1, 1.90);
IF NOT EXISTS (SELECT 1 FROM RaceEntries WHERE RaceID = @RefFinishedRaceID AND HorseID = @Dmh3)
    INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed, Odds)
    VALUES (@RefFinishedRaceID, @Dmh3, @Jockey4ID, 3, 'Ready', 1, @OrganizerUserID, 1, 2.25);

DECLARE @RefFinishedEntry1 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @RefFinishedRaceID AND HorseID = @HorseBetB);
DECLARE @RefFinishedEntry2 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @RefFinishedRaceID AND HorseID = @Dmh1);
DECLARE @RefFinishedEntry3 INT = (SELECT EntryID FROM RaceEntries WHERE RaceID = @RefFinishedRaceID AND HorseID = @Dmh3);

IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @RefFinishedRaceID AND EntryID = @RefFinishedEntry1)
    INSERT INTO RaceResults (RaceID, EntryID, FinishTime, FinishPosition, DNF, DQ, ConfirmedByRef, ConfirmedAt, ApprovalStatus, ApprovedByOrganizer, ApprovedAt)
    VALUES (@RefFinishedRaceID, @RefFinishedEntry1, 88.500, 1, 0, 0, @RefereeID, GETDATE(), 'Approved', @OrganizerUserID, GETDATE());
IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @RefFinishedRaceID AND EntryID = @RefFinishedEntry2)
    INSERT INTO RaceResults (RaceID, EntryID, FinishTime, FinishPosition, DNF, DQ, ConfirmedByRef, ConfirmedAt, ApprovalStatus, ApprovedByOrganizer, ApprovedAt)
    VALUES (@RefFinishedRaceID, @RefFinishedEntry2, 87.900, 2, 0, 0, @RefereeID, GETDATE(), 'Approved', @OrganizerUserID, GETDATE());
IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @RefFinishedRaceID AND EntryID = @RefFinishedEntry3)
    INSERT INTO RaceResults (RaceID, EntryID, FinishTime, FinishPosition, DNF, DQ, ConfirmedByRef, ConfirmedAt, ApprovalStatus, ApprovedByOrganizer, ApprovedAt)
    VALUES (@RefFinishedRaceID, @RefFinishedEntry3, 90.100, 3, 0, 0, @RefereeID, GETDATE(), 'Approved', @OrganizerUserID, GETDATE());

IF NOT EXISTS (SELECT 1 FROM Violations WHERE RaceID = @RefFinishedRaceID AND EntryID = @RefFinishedEntry2 AND ViolationType = 'XuatPhatSai')
    INSERT INTO Violations (RaceID, EntryID, RefereeID, ViolationType, PenaltySeconds, IsDQ, EvidenceImageURL, Description)
    VALUES (@RefFinishedRaceID, @RefFinishedEntry2, @RefereeID, 'XuatPhatSai', 3.00, 0, N'demo-evidence/ref1-xuat-phat-sai.jpg', N'False start, plus 3 seconds.');

EXEC sp_UpdateRaceResultRanking @RefFinishedRaceID;

IF NOT EXISTS (SELECT 1 FROM RaceMinutes WHERE RaceID = @RefFinishedRaceID)
    INSERT INTO RaceMinutes (RaceID, RefereeID, Content, WeatherCondition, PreRaceChecks, PostRaceNotes, MinutesFileURL, SentToOwners, SentAt)
    VALUES (@RefFinishedRaceID, @RefereeID,
            N'Referee1 demo: race finished and results were approved by Organizer but not published yet.',
            N'Clear weather',
            N'Horses, jockeys, starting lanes, and health status were checked before the race.',
            N'One false start violation was recorded. The system added 3 seconds and recalculated ranking.',
            N'demo-uploads/referee1-minutes.webp', 1, GETDATE());

INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @RefereeUserID, N'REF1: Pre-race assignment', N'You are the chief referee for REF1_PRECHECK_RACE.', 'RefereeAssigned', @RefPreRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefPreRaceID);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @RefereeUserID, N'REF1: Ongoing race assignment', N'You need to enter results and record violations for REF1_ONGOING_INPUT_RACE.', 'RefereeAssigned', @RefOngoingRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefOngoingRaceID);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @RefereeUserID, N'REF1: Race minutes ready', N'REF1_FINISHED_MINUTES_RACE has results, violations, and race minutes for demo.', 'RefereeAssigned', @RefFinishedRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefFinishedRaceID);

-- 14) Demo helper: print IDs for Postman variables.
PRINT N'============================================================';
PRINT N'DEMO 5 FLOW VARIABLES';
SELECT
    @FlowTournamentID AS tournamentId,
    @FlowRoundID AS roundId,
    @OwnerRaceID AS ownerRaceId,
    @RefRaceID AS refereeRaceId,
    @BetRaceID AS bettingRaceId,
    @OrgRaceID AS organizerRaceId,
    @OwnerEntryID AS ownerEntryId,
    @RefEntry1ID AS refereeEntryId,
    @BetEntry1ID AS bettingEntryId,
    @OrgPendingEntryID AS organizerPendingEntryId,
    @HorseInvite AS horseId,
    @Jockey2ID AS jockeyId,
    @RefereeID AS refereeId;
PRINT N'============================================================';
PRINT N' HorseRacingDB v2 created successfully!';
PRINT N' Business Flow v3 synchronized with settle-on-publish, DQ, and prize-to-wallet fixes.';
PRINT N' Phase 1-8 schema included: admin user filters, direct tournament flow, race status flow, entry health approval, jockey deals, assigned referee races, and pre-race checks.';
PRINT N'============================================================';
GO





/* ============================================================
   PHASE 1 -> 6 DATA NORMALIZATION
   File: DTB_8.2.2026.sql

   Business state after phase 6:
   - Tournament approval by Admin is removed.
   - Tournaments use Draft/Open/Ongoing/Finished/Cancelled.
   - Every tournament has exactly 3 default rounds:
       1. Qualify
       2. Semi Final
       3. Final
   - Race statuses use Draft, RegistrationOpen, Ongoing, Finished, Cancelled.
   - Old race status Scheduled is migrated to Draft.
   - One round has at most one race.
   - Horse health and active flags are aligned.
   ============================================================ */

USE HorseRacingDB;
GO

BEGIN TRANSACTION;

UPDATE dbo.Races
SET Status = 'Draft'
WHERE Status = 'Scheduled';

UPDATE dbo.Tournaments
SET Status = 'Draft'
WHERE Status = 'PendingApproval'
   OR Status IS NULL
   OR LTRIM(RTRIM(Status)) = '';

UPDATE dbo.Horses
SET HealthStatus = 'Active'
WHERE HealthStatus IS NULL
   OR LTRIM(RTRIM(HealthStatus)) = '';

UPDATE dbo.Horses
SET IsActive = 0
WHERE HealthStatus = 'Inactive';

UPDATE dbo.Horses
SET IsActive = 1
WHERE HealthStatus IN ('Active', 'Injured');

INSERT INTO dbo.Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate)
SELECT
    t.TournamentID,
    v.RoundName,
    v.RoundOrder,
    t.StartDate,
    t.EndDate
FROM dbo.Tournaments t
CROSS APPLY (
    VALUES
        ('Qualify', 1),
        ('Semi Final', 2),
        ('Final', 3)
) v(RoundName, RoundOrder)
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.Rounds r
    WHERE r.TournamentID = t.TournamentID
      AND r.RoundOrder = v.RoundOrder
);

UPDATE dbo.Rounds
SET RoundName = 'Qualify'
WHERE RoundOrder = 1
  AND RoundName <> 'Qualify';

UPDATE dbo.Rounds
SET RoundName = 'Semi Final'
WHERE RoundOrder = 2
  AND RoundName <> 'Semi Final';

UPDATE dbo.Rounds
SET RoundName = 'Final'
WHERE RoundOrder = 3
  AND RoundName <> 'Final';

DELETE r
FROM dbo.Rounds r
WHERE r.RoundOrder NOT IN (1, 2, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.Races ra
      WHERE ra.RoundID = r.RoundID
  );

;WITH race_order AS (
    SELECT
        ra.RaceID,
        ra.TournamentID,
        ROW_NUMBER() OVER (
            PARTITION BY ra.TournamentID
            ORDER BY ra.RaceDate, ra.RaceID
        ) AS rn
    FROM dbo.Races ra
),
target_round AS (
    SELECT
        r.TournamentID,
        r.RoundID,
        r.RoundOrder
    FROM dbo.Rounds r
    WHERE r.RoundOrder IN (1, 2, 3)
)
UPDATE ra
SET ra.RoundID = tr.RoundID
FROM dbo.Races ra
JOIN race_order ro
    ON ro.RaceID = ra.RaceID
JOIN target_round tr
    ON tr.TournamentID = ro.TournamentID
   AND tr.RoundOrder = ro.rn
WHERE ro.rn <= 3;

;WITH race_order AS (
    SELECT
        RaceID,
        ROW_NUMBER() OVER (
            PARTITION BY TournamentID
            ORDER BY RaceDate, RaceID
        ) AS rn
    FROM dbo.Races
)
UPDATE ra
SET
    ra.Status = 'Cancelled',
    ra.RoundID = NULL
FROM dbo.Races ra
JOIN race_order ro
    ON ro.RaceID = ra.RaceID
WHERE ro.rn > 3;

COMMIT TRANSACTION;
GO

PRINT 'DTB_8.2.2026 phase 6 normalization completed.';
GO

SELECT Status, COUNT(*) AS Total
FROM dbo.Races
GROUP BY Status
ORDER BY Status;

SELECT Status, COUNT(*) AS Total
FROM dbo.Tournaments
GROUP BY Status
ORDER BY Status;

SELECT TournamentID, COUNT(*) AS RoundCount
FROM dbo.Rounds
GROUP BY TournamentID
HAVING COUNT(*) <> 3
ORDER BY TournamentID;

SELECT RoundID, COUNT(*) AS RaceCount
FROM dbo.Races
WHERE RoundID IS NOT NULL
GROUP BY RoundID
HAVING COUNT(*) > 1
ORDER BY RoundID;
GO
