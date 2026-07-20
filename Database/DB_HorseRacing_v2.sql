-- ============================================================
--  HORSE RACING TOURNAMENT MANAGEMENT SYSTEM — DATABASE v2
--  MS SQL Server · Đã đồng bộ Business Flow v3 + fix bug BE
--  Thay thế bản DB26.6.2026 (drop & recreate cho môi trường dev)
-- ============================================================
--  THAY ĐỔI CHÍNH so với bản cũ:
--   1. 1 role Organizer duy nhất (bỏ OrganizerHead/Member)
--   2. Tournament: status PendingApproval; Organizer tạo → Admin duyệt
--   3. RaceEntries: OrganizerApproved (bỏ OwnerConfirmed/AdminApproved dư)
--   4. RaceResults: + PenaltyTime, + FinalTime (computed) → xếp hạng theo FinalTime
--   5. Violations: PenaltySeconds (số giây) + EvidenceImageURL (ảnh bằng chứng)
--   6. RaceMinutes: + MinutesFileURL (ảnh biên bản ký tay)
--   7. Races.Status: thêm RegistrationOpen (Trọng tài đổi trạng thái)
--   8. SP: settle cược ở PUBLISH + loại DQ; credit thưởng ví Owner; auto-tạo ví
--   9. HealthStatus ngựa: chỉ Organizer/BTC cập nhật (cột HealthUpdatedBy)
-- ============================================================

-- Bắt buộc ON: bảng có computed column (RaceResults.FinalTime, HorseTournamentStats.TotalPoints/Wins)
-- yêu cầu các SET option này. Đặt ở đầu để chạy được ở mọi client (SSMS, sqlcmd, ...).
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
    PasswordHash  NVARCHAR(256)  NOT NULL,          -- BCrypt (KHÔNG lưu plaintext)
    FullName      NVARCHAR(150)  NOT NULL,
    Email         NVARCHAR(150)  NOT NULL UNIQUE,
    Phone         NVARCHAR(20),
    AvatarURL     NVARCHAR(500),
    RoleID        INT            NOT NULL REFERENCES Roles(RoleID),   -- 1 user = 1 role
    IsActive      BIT            NOT NULL DEFAULT 1,
    IsApproved    BIT            NOT NULL DEFAULT 0,   -- Admin duyệt tài khoản
    FailedLoginAttempts INT      NOT NULL DEFAULT 0,
    IsLocked      BIT            NOT NULL DEFAULT 0,
    LastLogin     DATETIME2      NULL,
    ResetToken    NVARCHAR(255)  NULL,
    ResetTokenExpiry DATETIME2   NULL,
    RejectReason  NVARCHAR(500)  NULL,          -- (1) lý do Admin từ chối tài khoản
    IsSystemAdmin BIT            NOT NULL DEFAULT 0,  -- (2) Hard Admin: KHÔNG thể bị đổi role/khoá/xoá
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

-- Refresh tokens (JWT). userId lấy từ JWT server-side (KHÔNG dùng header X-User-Id).
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
--    (owner-tự-làm-jockey ĐÃ BỎ: mỗi user 1 role, không cross-record)
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
    ApprovalStatus NVARCHAR(30)  NOT NULL DEFAULT 'Approved', -- Admin duyệt hồ sơ jockey
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
-- 3. HORSES  (HealthStatus: Hoat dong / Bi thuong / Khong hoat dong)
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
    RegisterCode  NVARCHAR(50),                 -- BE tự sinh
    HealthStatus  NVARCHAR(100) NOT NULL DEFAULT N'Hoạt động',
    HealthUpdatedBy INT         NULL REFERENCES Users(UserID),  -- CHỈ Organizer/BTC cập nhật (khám offline)
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
    RecordedBy INT           NULL REFERENCES Users(UserID),  -- Organizer/BTC nhập phiếu khám
    CreatedAt  DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 4. TOURNAMENTS & ROUNDS
--    Organizer TẠO (CreatedBy) → gửi Admin duyệt (ApprovedByAdmin)
-- ============================================================
CREATE TABLE Tournaments (
    TournamentID   INT IDENTITY(1,1) PRIMARY KEY,
    TournamentName NVARCHAR(200) NOT NULL,
    Description    NVARCHAR(1000),
    Location       NVARCHAR(300),
    StartDate      DATE          NOT NULL,
    EndDate        DATE          NOT NULL,
    BudgetTotal    DECIMAL(18,2) DEFAULT 0,   -- (8) đổi tên PrizeFund → BudgetTotal (khớp API)
    MaxHorses      INT           NULL,
    MaxParticipants INT          NULL,        -- (8) thêm cho khớp API maxParticipants
    Status         NVARCHAR(30)  NOT NULL DEFAULT 'Draft',
    -- Draft | PendingApproval | Open | Ongoing | Finished | Cancelled
    CreatedBy      INT           REFERENCES Users(UserID),   -- Organizer tạo
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
-- 5. RACES  (Status: Scheduled | RegistrationOpen | Ongoing | Finished | Cancelled)
--    Trọng tài đổi trạng thái (Đang diễn ra → Kết thúc) — kiểm soát ở tầng app.
-- ============================================================
CREATE TABLE Races (
    RaceID          INT IDENTITY(1,1) PRIMARY KEY,
    TournamentID    INT           NOT NULL REFERENCES Tournaments(TournamentID),
    RoundID         INT           REFERENCES Rounds(RoundID),
    RaceName        NVARCHAR(200) NOT NULL,
    RaceDate        DATETIME2     NOT NULL,
    TrackLength     INT,            -- mét
    TrackType       NVARCHAR(50),
    MaxParticipants INT,
    PrizeFirst      DECIMAL(18,2)  DEFAULT 0,
    PrizeSecond     DECIMAL(18,2)  DEFAULT 0,
    PrizeThird      DECIMAL(18,2)  DEFAULT 0,
    Status          NVARCHAR(30)   NOT NULL DEFAULT 'Scheduled',
    RegistrationOpen  DATETIME2    NULL,
    RegistrationClose DATETIME2    NULL,
    CreatedAt       DATETIME2      DEFAULT GETDATE(),
    UpdatedAt       DATETIME2      DEFAULT GETDATE()
);
-- Phân công trọng tài cho cuộc đua (do Organizer phân công)
CREATE TABLE RaceReferees (
    RaceRefereeID INT IDENTITY(1,1) PRIMARY KEY,
    RaceID        INT NOT NULL REFERENCES Races(RaceID),
    RefereeID     INT NOT NULL REFERENCES Referees(RefereeID),
    Role          NVARCHAR(50),   -- Chief / Assistant
    AssignedAt    DATETIME2 DEFAULT GETDATE(),
    UNIQUE (RaceID, RefereeID)
);
-- (7) Lịch sử đổi trạng thái cuộc đua — Referee là người đổi, lưu ai/khi nào
CREATE TABLE RaceStatusHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    RaceID    INT NOT NULL REFERENCES Races(RaceID),
    OldStatus NVARCHAR(30),
    NewStatus NVARCHAR(30) NOT NULL,
    ChangedBy INT REFERENCES Users(UserID),
    ChangedAt DATETIME2 DEFAULT GETDATE()
);

-- ============================================================
-- 6. RACE ENTRIES  (Organizer duyệt: OrganizerApproved)
-- ============================================================
CREATE TABLE RaceEntries (
    EntryID            INT IDENTITY(1,1) PRIMARY KEY,
    RaceID             INT NOT NULL REFERENCES Races(RaceID),
    HorseID            INT NOT NULL REFERENCES Horses(HorseID),
    JockeyID           INT NULL REFERENCES Jockeys(JockeyID),
    LaneNumber         INT,
    RegistrationStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending', -- (3) NGUỒN CHÂN LÝ: Pending|Approved|Rejected|Withdrawn|Ready
    OrganizerApproved  BIT          NOT NULL DEFAULT 0,          -- cờ phụ — BE PHẢI set đồng bộ với RegistrationStatus
    ApprovedBy         INT          NULL REFERENCES Users(UserID),  -- ai duyệt (Approved/Ready = Organizer đã duyệt)
    RejectReason       NVARCHAR(500) NULL,
    JockeyConfirmed    BIT          NOT NULL DEFAULT 0,          -- jockey nhận lời mời (Approved + JockeyConfirmed → BE set 'Ready')
    Odds               DECIMAL(10,2) NOT NULL DEFAULT 2.00,     -- (5) tỉ lệ cược server-side, chống FE gửi odds tự do
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
    Status         NVARCHAR(20)  NOT NULL DEFAULT 'Pending',  -- Pending|Accepted|Declined
    InvitedAt      DATETIME2     DEFAULT GETDATE(),
    RespondedAt    DATETIME2
);

-- ============================================================
-- 8. RACE RESULTS
--    Trọng tài nhập FinishTime; hệ thống tính FinalTime = FinishTime + PenaltyTime.
--    Xếp hạng (FinishPosition) theo FinalTime tăng dần, DQ/DNF xếp cuối.
-- ============================================================
CREATE TABLE RaceResults (
    ResultID       INT IDENTITY(1,1) PRIMARY KEY,
    RaceID         INT           NOT NULL REFERENCES Races(RaceID),
    EntryID        INT           NOT NULL REFERENCES RaceEntries(EntryID),
    FinishTime     DECIMAL(10,3) NULL,      -- giây (giờ về đích vật lý)
    PenaltyTime    DECIMAL(10,3) NOT NULL DEFAULT 0,  -- tổng giây phạt (từ Violations)
    FinalTime      AS (CASE WHEN FinishTime IS NULL THEN NULL ELSE FinishTime + PenaltyTime END) PERSISTED,
    FinishPosition INT           NULL,      -- do hệ thống tính từ FinalTime
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
-- 9. VIOLATIONS  (PenaltySeconds số + ảnh bằng chứng)
--    Rule: Xuất phát sai +3s · Lấn lane +5s · Cản đường +10s · Vi phạm nặng → DQ
-- ============================================================
CREATE TABLE Violations (
    ViolationID      INT IDENTITY(1,1) PRIMARY KEY,
    RaceID           INT           NOT NULL REFERENCES Races(RaceID),
    EntryID          INT           NOT NULL REFERENCES RaceEntries(EntryID),
    RefereeID        INT           NOT NULL REFERENCES Referees(RefereeID),
    ViolationType    NVARCHAR(100) NOT NULL,   -- XuatPhatSai | LanLane | CanDuong | ViPhamNang
    PenaltySeconds   DECIMAL(5,2)  NOT NULL DEFAULT 0,  -- 0 nếu là DQ
    IsDQ             BIT           NOT NULL DEFAULT 0,
    EvidenceImageURL NVARCHAR(500),            -- ảnh bằng chứng
    Description      NVARCHAR(1000),
    RecordedAt       DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 10. RACE MINUTES / BIÊN BẢN  (+ ảnh biên bản ký tay + gửi Owner)
-- ============================================================
CREATE TABLE RaceMinutes (
    MinuteID       INT IDENTITY(1,1) PRIMARY KEY,
    RaceID         INT           NOT NULL UNIQUE REFERENCES Races(RaceID),
    RefereeID      INT           NOT NULL REFERENCES Referees(RefereeID),
    Content        NVARCHAR(MAX),
    PreRaceChecks  NVARCHAR(MAX),
    PostRaceNotes  NVARCHAR(MAX),
    MinutesFileURL NVARCHAR(500),   -- ảnh/PDF biên bản đã có chữ ký TẤT CẢ jockey
    SentToOwners   BIT           NOT NULL DEFAULT 0,  -- đã gửi toàn bộ Owner chưa
    SentAt         DATETIME2     NULL,
    CreatedAt      DATETIME2     DEFAULT GETDATE(),
    UpdatedAt      DATETIME2     DEFAULT GETDATE()
);

-- ============================================================
-- 11. LEADERBOARD / RANKING
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
    -- Bí danh cho BE HorseRepository.findHorseRank (đọc stats.TotalPoints / stats.Wins).
    -- Computed column → luôn khớp Points/TotalWins, không cần proc cập nhật thêm.
    TotalPoints  AS (Points),
    Wins         AS (TotalWins),
    UpdatedAt    DATETIME2 DEFAULT GETDATE(),
    UNIQUE (TournamentID, HorseID)
);

-- ============================================================
-- 12. WALLET + BETTING
-- ============================================================
CREATE TABLE Wallets (
    WalletID  INT IDENTITY(1,1) PRIMARY KEY,
    UserID    INT           NOT NULL UNIQUE REFERENCES Users(UserID),
    Balance   DECIMAL(18,2) NOT NULL DEFAULT 0,
    CreatedAt DATETIME2     DEFAULT GETDATE(),
    UpdatedAt DATETIME2     DEFAULT GETDATE()
);
CREATE TABLE WalletTransactions (
    TransactionID   INT IDENTITY(1,1) PRIMARY KEY,
    WalletID        INT           NOT NULL REFERENCES Wallets(WalletID),
    Amount          DECIMAL(18,2) NOT NULL,          -- QUY ƯỚC DẤU: Deposit(+) BetPlaced(-) BetWon(+) BetRefund(+) PrizeAwarded(+)
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
    BetType         NVARCHAR(30)  NOT NULL DEFAULT 'WIN',   -- WIN(nhất) | PLACE(top2) | SHOW(top3) | EXACT(đúng hạng)
    TargetPosition  INT           NULL,                     -- CHỈ dùng cho EXACT: cược con này về hạng mấy
    Amount          DECIMAL(18,2) NOT NULL,
    Odds            DECIMAL(10,2) NOT NULL DEFAULT 2.00,    -- odds theo BetType (snapshot lúc đặt; harder = higher)
    PotentialPayout DECIMAL(18,2) NOT NULL,                 -- = Amount × Odds
    Status          NVARCHAR(30)  NOT NULL DEFAULT 'Pending', -- Pending|Won|Lost|Cancelled
    CreatedAt       DATETIME2     DEFAULT GETDATE(),
    SettledAt       DATETIME2       -- (đã BỎ UNIQUE(UserID,RaceID): cho đặt nhiều vé/race miễn đủ tiền)
);

-- ============================================================
-- 13. NOTIFICATIONS  (hỗ trợ gửi hàng loạt cho toàn bộ Owner)
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
('Admin',      N'Quản trị hệ thống, duyệt tài khoản & giải đấu'),
('HorseOwner', N'Chủ ngựa'),
('Jockey',     N'Nài ngựa'),
('Referee',    N'Trọng tài — nhập kết quả, đổi trạng thái cuộc đua'),
('Spectator',  N'Khán giả — đặt cược'),
('Organizer',  N'Ban tổ chức — tạo giải, duyệt đăng ký, phân công trọng tài, công bố');

INSERT INTO Permissions (PermissionName, Description) VALUES
('user.manage', N'Quản lý tài khoản'),
('tournament.create', N'Organizer tạo giải'),
('tournament.approve', N'Admin duyệt giải'),
('entry.approve', N'Organizer duyệt đăng ký'),
('race.status.update', N'Trọng tài đổi trạng thái cuộc đua'),
('result.enter', N'Trọng tài nhập kết quả + vi phạm'),
('result.approve', N'Organizer duyệt kết quả'),
('result.publish', N'Organizer công bố kết quả'),
('referee.assign', N'Organizer phân công trọng tài'),
('horse.manage', N'Chủ ngựa quản lý ngựa'),
('health.update', N'Organizer/BTC cập nhật sức khỏe ngựa'),
('jockey.invite', N'Chủ ngựa mời jockey'),
('bet.create', N'Khán giả đặt cược'),
('config.manage', N'Admin quản lý cấu hình');

-- Gán quyền
INSERT INTO RolePermissions (RoleID, PermissionID)
SELECT 1, PermissionID FROM Permissions;  -- Admin: tất cả
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
('MAINTENANCE_MODE',  '0',    N'1 = bảo trì (chỉ Admin đăng nhập), 0 = hoạt động'),
('MAINTENANCE_UNTIL', NULL,   N'Thời gian dự kiến mở lại'),
('DEFAULT_BET_ODDS',  '2.00', N'Tỉ lệ cược mặc định'),
('PENALTY_XuatPhatSai','3',   N'Phạt xuất phát sai (giây)'),
('PENALTY_LanLane',   '5',    N'Phạt lấn lane (giây)'),
('PENALTY_CanDuong',  '10',   N'Phạt cản đường (giây)'),
('ODDS_FACTOR_PLACE', '0.50', N'Odds PLACE (top2)  = WIN odds × hệ số này'),
('ODDS_FACTOR_SHOW',  '0.35', N'Odds SHOW (top3)   = WIN odds × hệ số này'),
('ODDS_FACTOR_EXACT', '1.50', N'Odds EXACT (đúng hạng) = WIN odds × hệ số này'),
('ODDS_MIN',          '1.10', N'Odds sàn tối thiểu cho mọi kiểu cược');
GO

-- ============================================================
-- STORED PROCEDURES (đã fix bug)
-- ============================================================

-- Admin duyệt tài khoản → auto-tạo ví (mọi role đều có ví để nhận thưởng)
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

-- Tính xếp hạng theo FinalTime (DQ/DNF xếp cuối), gán Points + PrizeWon
CREATE PROCEDURE sp_ComputeRaceRanking @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    -- Xếp hạng ngựa hợp lệ theo FinalTime tăng dần
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
    -- DQ/DNF: không xếp hạng, không điểm/thưởng
    UPDATE RaceResults SET FinishPosition = NULL, Points = 0, PrizeWon = 0
    WHERE RaceID = @RaceID AND (DQ = 1 OR DNF = 1);
END;
GO

-- Settle cược: winner = ngựa hạng 1 (FinalTime nhỏ nhất, KHÔNG DQ/DNF)
CREATE PROCEDURE sp_SettleBets @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Now DATETIME2 = GETDATE(), @Outer INT = @@TRANCOUNT;
    -- Cần đã tính xếp hạng (FinishPosition) mới settle được
    IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @RaceID AND FinishPosition IS NOT NULL) RETURN;

    BEGIN TRY
        IF @Outer = 0 BEGIN TRANSACTION;
        -- Đánh giá TỪNG vé theo BetType, dựa vào FinishPosition của con ngựa được cược
        UPDATE b
        SET Status = CASE
              WHEN rr.FinishPosition IS NULL                                   THEN 'Lost'  -- ngựa DQ/DNF → thua mọi kiểu
              WHEN b.BetType = 'WIN'   AND rr.FinishPosition = 1               THEN 'Won'    -- về nhất
              WHEN b.BetType = 'PLACE' AND rr.FinishPosition <= 2              THEN 'Won'    -- top 2
              WHEN b.BetType = 'SHOW'  AND rr.FinishPosition <= 3              THEN 'Won'    -- top 3
              WHEN b.BetType = 'EXACT' AND rr.FinishPosition = b.TargetPosition THEN 'Won'   -- đúng hạng
              ELSE 'Lost' END,
            SettledAt = @Now
        FROM Bets b
        LEFT JOIN RaceResults rr ON rr.RaceID = b.RaceID AND rr.EntryID = b.EntryID
        WHERE b.RaceID = @RaceID AND b.Status = 'Pending';

        -- Cộng tiền thắng cược — GỘP TỔNG theo user (payout đã snapshot theo odds từng kiểu lúc đặt)
        UPDATE w SET Balance = w.Balance + agg.Total, UpdatedAt = GETDATE()
        FROM Wallets w
        JOIN (SELECT UserID, SUM(PotentialPayout) AS Total FROM Bets
              WHERE RaceID = @RaceID AND Status = 'Won' AND SettledAt = @Now
              GROUP BY UserID) agg ON agg.UserID = w.UserID;

        INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
        SELECT w.WalletID, b.PotentialPayout, 'BetWon', N'Thắng cược sau khi kết quả được công bố', 'Bet', b.BetID
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

-- Credit tiền thưởng vào ví Owner (theo PrizeWon từng ngựa)
CREATE PROCEDURE sp_AwardOwnerPrizes @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Outer INT = @@TRANCOUNT;
    BEGIN TRY
        IF @Outer = 0 BEGIN TRANSACTION;
        -- Cộng thưởng vào ví chủ ngựa — GỘP TỔNG (owner có thể có nhiều ngựa trúng thưởng)
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
        SELECT w.WalletID, rr.PrizeWon, 'PrizeAwarded', N'Tiền thưởng thứ hạng cuộc đua', 'Race', @RaceID
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

-- Organizer CÔNG BỐ kết quả → tính hạng + settle cược + credit thưởng owner (TẤT CẢ ở bước publish)
CREATE PROCEDURE sp_PublishRaceResult @RaceID INT, @OrganizerID INT
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT EXISTS (SELECT 1 FROM RaceResults WHERE RaceID = @RaceID AND ApprovalStatus = 'Approved')
    BEGIN
        RAISERROR(N'Kết quả chưa được Organizer duyệt, không thể công bố.', 16, 1); RETURN;
    END

    DECLARE @Outer INT = @@TRANCOUNT;
    BEGIN TRY
        IF @Outer = 0 BEGIN TRANSACTION;
        EXEC sp_ComputeRaceRanking @RaceID;   -- 1. xếp hạng theo FinalTime

        UPDATE RaceResults SET ApprovalStatus = 'Published', PublishedAt = GETDATE()  -- 2. publish
        WHERE RaceID = @RaceID AND ApprovalStatus = 'Approved';
        UPDATE Races SET Status = 'Finished', UpdatedAt = GETDATE() WHERE RaceID = @RaceID;

        EXEC sp_SettleBets @RaceID;        -- 3. settle cược Ở PUBLISH
        EXEC sp_AwardOwnerPrizes @RaceID;  -- 4. credit thưởng ví owner

        -- 5. Thông báo owner
        INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
        SELECT DISTINCT u.UserID, N'Kết quả cuộc đua đã công bố', N'Kết quả vừa được công bố, kiểm tra bảng xếp hạng.', 'ResultPublished', @RaceID, 'Race'
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

-- Trọng tài gửi biên bản cho TOÀN BỘ Owner có ngựa dự đua
CREATE PROCEDURE sp_SendMinutesToOwners @RaceID INT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
    SELECT DISTINCT u.UserID, N'Biên bản cuộc đua', N'Trọng tài đã gửi biên bản (kèm ảnh chữ ký) cho bạn.', 'MinutesSent', @RaceID, 'Race'
    FROM RaceEntries re
    JOIN Horses h ON re.HorseID = h.HorseID
    JOIN HorseOwners ho ON h.OwnerID = ho.OwnerID
    JOIN Users u ON ho.UserID = u.UserID
    WHERE re.RaceID = @RaceID;
    UPDATE RaceMinutes SET SentToOwners = 1, SentAt = GETDATE() WHERE RaceID = @RaceID;
END;
GO

-- (4) Huỷ cuộc đua → hoàn tiền TẤT CẢ cược (chỉ refund khi race bị huỷ)
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

        -- Hoàn tiền các cược đang Pending vào ví — GỘP TỔNG theo user
        UPDATE w SET Balance = w.Balance + agg.Total, UpdatedAt = GETDATE()
        FROM Wallets w
        JOIN (SELECT UserID, SUM(Amount) AS Total FROM Bets
              WHERE RaceID = @RaceID AND Status = 'Pending'
              GROUP BY UserID) agg ON agg.UserID = w.UserID;

        INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
        SELECT w.WalletID, b.Amount, 'BetRefund', N'Hoàn tiền do cuộc đua bị huỷ', 'Bet', b.BetID
        FROM Bets b JOIN Wallets w ON w.UserID = b.UserID
        WHERE b.RaceID = @RaceID AND b.Status = 'Pending';

        UPDATE Bets SET Status = 'Cancelled', SettledAt = @Now WHERE RaceID = @RaceID AND Status = 'Pending';

        INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
        SELECT DISTINCT b.UserID, N'Cuộc đua bị huỷ', N'Cuộc đua bị huỷ, tiền cược đã được hoàn vào ví.', 'RaceCancelled', @RaceID, 'Race'
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
-- TRIGGER: tự cộng PenaltyTime + set DQ từ Violations
--   → FinalTime (computed) tự tính lại; BE khỏi cần nhớ cộng tay.
--   Quy ước: PenaltySeconds là giây phạt; IsDQ=1 nghĩa vi phạm nặng → DQ.
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
-- Dashboard tổng quan cho Admin (BE: DashboardService đọc đúng 9 cột này)
CREATE VIEW vw_SystemDashboard AS
SELECT
  (SELECT COUNT(*) FROM Users   WHERE IsActive = 1)                                        AS TotalActiveUsers,
  (SELECT COUNT(*) FROM Users   WHERE IsApproved = 0)                                      AS PendingApprovals,
  (SELECT COUNT(*) FROM Tournaments WHERE Status = 'Ongoing')                              AS OngoingTournaments,
  (SELECT COUNT(*) FROM Races   WHERE Status IN ('Scheduled','RegistrationOpen','Ongoing')) AS UpcomingRaces,
  (SELECT COUNT(*) FROM Races   WHERE Status = 'Finished')                                 AS FinishedRaces,
  (SELECT COUNT(*) FROM Horses)                                                            AS TotalHorses,
  (SELECT COUNT(*) FROM Jockeys)                                                           AS TotalJockeys,
  (SELECT COUNT(*) FROM Bets)                                                              AS TotalBets,
  (SELECT COUNT(*) FROM Bets    WHERE Status = 'Won')                                      AS WonBets;
GO

-- ============================================================
-- DEMO SEED (đủ để test luồng chính)
-- ============================================================
-- Mật khẩu của TẤT CẢ user demo bên dưới: 123456
-- Hash BCrypt thật (do chính BE sinh ra) → login được ngay, không cần đổi gì.
DECLARE @Pwd NVARCHAR(100) = '$2a$10$6rvu1cSRS60NNTQtJQZpYO34ZCaJ73I8dFDvXdw4BxYzrlKFKhTq6';

INSERT INTO Users (Username, PasswordHash, FullName, Email, Phone, RoleID, IsActive, IsApproved) VALUES
('admin',      @Pwd, N'System Admin',        'admin@gmail.com',      '0900000001', 1, 1, 1),
('organizer1', @Pwd, N'Ban Tổ Chức A',        'organizer1@gmail.com', '0900000002', 6, 1, 1),
('owner1',     @Pwd, N'Nguyễn Văn Owner A',   'owner1@gmail.com',     '0900000003', 2, 1, 1),
('owner2',     @Pwd, N'Trần Thị Owner B',      'owner2@gmail.com',     '0900000004', 2, 1, 1),
('jockey1',    @Pwd, N'Lê Văn Jockey C',       'jockey1@gmail.com',    '0900000005', 3, 1, 1),
('jockey2',    @Pwd, N'Phạm Văn Jockey D',     'jockey2@gmail.com',    '0900000006', 3, 1, 1),
('referee1',   @Pwd, N'Võ Văn Referee E',      'referee1@gmail.com',   '0900000007', 4, 1, 1),
('spectator1', @Pwd, N'Hoàng Văn Spectator',   'spectator1@gmail.com', '0900000008', 5, 1, 1),
('owner_pending', @Pwd, N'Lâm Văn Chờ Duyệt',  'pending@gmail.com',    '0900000009', 2, 1, 0);

INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber) VALUES
(3, '079205000001', N'Hồ Chí Minh', N'Black Stable', 'OWN001'),
(4, '079205000002', N'Đà Nẵng',     N'Golden Stable','OWN002');
DECLARE @OwnerPendingUserID INT = (SELECT UserID FROM Users WHERE Username = 'owner_pending');
IF @OwnerPendingUserID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM HorseOwners WHERE UserID = @OwnerPendingUserID)
BEGIN
    INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber)
    VALUES (@OwnerPendingUserID, '079205000009', N'Hồ Chí Minh', N'Pending Stable', 'OWN-PENDING');
END;
INSERT INTO Jockeys (UserID, NationalID, LicenseNumber, WeightKg, HeightCm, ExperienceYear) VALUES
(5, '079205000003', 'JK001', 55.5, 170, 5),
(6, '079205000004', 'JK002', 53.0, 168, 4);
INSERT INTO Referees (UserID, BadgeNumber, Speciality) VALUES
(7, 'REF001', N'Chief Referee');

-- Ngựa: owner1 (OwnerID 1) có Hắc Phong/Bão Lửa/Thần Mã/Xích Thố; owner2 (OwnerID 2) có Tia Chớp/Kim Long
-- HealthStatus lưu đúng chuỗi backend dùng khi Organizer duyệt: N'Hoạt động'.
INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive) VALUES
(1, N'Hắc Phong', N'Thoroughbred', 2020, N'Đen',    N'Male',   450.5, 'HP001', N'Hoạt động', 1),  -- HorseID 1
(1, N'Bão Lửa',   N'Arabian',      2019, N'Nâu',    N'Male',   430.0, 'BL002', N'Hoạt động', 1),  -- 2
(2, N'Tia Chớp',  N'Mustang',      2021, N'Trắng',  N'Female', 410.0, 'TC003', N'Hoạt động', 1),  -- 3
(1, N'Thần Mã',   N'Warmblood',    2020, N'Xám',    N'Male',   445.0, 'TM004', N'Hoạt động', 1),  -- 4  ← owner1 CHƯA đăng ký → test "đăng ký thi đấu"
(2, N'Kim Long',  N'Andalusian',   2019, N'Vàng',   N'Female', 420.0, 'KL005', N'Hoạt động', 1),  -- 5
(1, N'Xích Thố',  N'Thoroughbred', 2018, N'Đỏ nâu', N'Male',   460.0, 'XT006', N'Hoạt động', 1);  -- 6

-- (2) Đánh dấu Hard Admin (không thể bị đổi/khoá)
UPDATE Users SET IsSystemAdmin = 1 WHERE Username = 'admin';

-- Ví: khán giả 2.000.000 để đặt cược; owner có sẵn 1.000.000 để thao tác
INSERT INTO Wallets (UserID, Balance)
SELECT UserID, CASE WHEN RoleID = 5 THEN 2000000 WHEN RoleID = 2 THEN 1000000 ELSE 0 END
FROM Users WHERE IsApproved = 1;

-- Giải đấu: T1 Open (Admin đã duyệt) · T2 PendingApproval (chờ Admin duyệt → test luồng Admin)
-- Ngày TƯƠNG ĐỐI theo GETDATE(): chạy seed lúc nào cuộc đua cũng ở tương lai → luôn đặt cược được.
INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt) VALUES
(N'Giải Đua Mùa Hè 2026', N'Giải đua ngựa mùa hè.',  N'Hồ Chí Minh',
 DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,14,CAST(GETDATE() AS DATE)),
 50000000, 20, 20, 'Open', 2, 1, GETDATE()),                                                        -- TournamentID 1
(N'Giải Đua Mùa Thu 2026', N'Organizer gửi, chờ Admin duyệt.', N'Đà Nẵng',
 DATEADD(DAY,20,CAST(GETDATE() AS DATE)), DATEADD(DAY,35,CAST(GETDATE() AS DATE)),
 40000000, 16, 16, 'PendingApproval', 2, NULL, NULL);                                               -- 2 ← Admin duyệt

INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate) VALUES
(1, N'Vòng loại', 1, DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,7,CAST(GETDATE() AS DATE)));  -- RoundID 1

-- 2 cuộc đua trong T1 (đều mở đăng ký, ngày tương lai)
INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose) VALUES
(1, 1, N'Race Opening', DATEADD(DAY,3,CAST(GETDATE() AS DATE)), 1200, N'Flat', 10, 20000000, 10000000, 5000000, 'RegistrationOpen',
 DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,2,CAST(GETDATE() AS DATE))),                   -- RaceID 1
(1, 1, N'Race Sprint 800m', DATEADD(DAY,5,CAST(GETDATE() AS DATE)), 800, N'Flat', 8, 12000000, 6000000, 3000000, 'RegistrationOpen',
 DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), DATEADD(DAY,4,CAST(GETDATE() AS DATE)));                   -- RaceID 2 ← owner đăng ký Thần Mã ở đây

-- Đăng ký ở Race 1 — đủ trạng thái để test mọi vai trò:
INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed) VALUES
(1, 1, 1,    1, 'Approved', 1, 2, 1),   -- EntryID 1: có jockey → đặt cược
(1, 3, 2,    2, 'Approved', 1, 2, 1),   -- 2: có jockey → đặt cược
(1, 2, NULL, 3, 'Approved', 1, 2, 0),   -- 3: owner1, ĐÃ duyệt nhưng CHƯA có jockey → test "mời Jockey"
(1, 5, NULL, 4, 'Pending',  0, NULL, 0),-- 4: owner2, CHỜ duyệt → test Organizer "duyệt đăng ký"
(1, 6, NULL, 5, 'Approved', 1, 2, 0);   -- 5: owner1, có lời mời Pending gửi jockey2 → test Jockey "nhận lời mời"

-- Phân công trọng tài cho Race 1 (Organizer → Referee). Referee thấy race này để điều khiển.
INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (1, 1, N'Chief');

-- Lời mời jockey đang chờ: owner1 mời jockey2 vào Entry 5 → jockey2 đăng nhập sẽ thấy & bấm nhận.
INSERT INTO JockeyInvitations (EntryID, JockeyID, InvitedByOwner, Message, Status) VALUES
(5, 2, 1, N'Mời bạn cầm cương Xích Thố ở Race Opening.', 'Pending');

-- Vài thông báo mẫu cho đúng vai
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity) VALUES
(7, N'Bạn được phân công trọng tài', N'Bạn là trọng tài chính của Race Opening.', 'RefereeAssigned', 1, 'Race'),
(6, N'Bạn có lời mời thi đấu',        N'Owner A mời bạn cầm cương Xích Thố.',       'InvitationReceived', 5, 'Entry'),
(2, N'Có đăng ký chờ duyệt',          N'Kim Long (owner2) đang chờ bạn duyệt.',      'EntryPendingApproval', 4, 'Entry');

-- ── Race 3: ĐÃ KẾT THÚC + có kết quả → test màn "Xem lại đường đua" (replay) ──
-- Kim Long về đích NHANH NHẤT (66.8s) nhưng bị DQ → replay minh hoạ "về đích ≠ hạng chính thức".
INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose) VALUES
(1, 1, N'Race Chung Kết', DATEADD(DAY,-1,CAST(GETDATE() AS DATE)), 1400, N'Flat', 10, 30000000, 15000000, 8000000, 'Finished',
 DATEADD(DAY,-10,CAST(GETDATE() AS DATE)), DATEADD(DAY,-2,CAST(GETDATE() AS DATE)));   -- RaceID 3

DECLARE @R3 INT = (SELECT RaceID FROM Races WHERE RaceName = N'Race Chung Kết');

INSERT INTO RaceEntries (RaceID, HorseID, JockeyID, LaneNumber, RegistrationStatus, OrganizerApproved, ApprovedBy, JockeyConfirmed) VALUES
(@R3, 1, 1,    1, 'Approved', 1, 2, 1),   -- Hắc Phong · Jockey C
(@R3, 2, 2,    2, 'Approved', 1, 2, 1),   -- Bão Lửa · Jockey D
(@R3, 3, NULL, 3, 'Approved', 1, 2, 0),   -- Tia Chớp
(@R3, 4, NULL, 4, 'Approved', 1, 2, 0),   -- Thần Mã
(@R3, 5, NULL, 5, 'Approved', 1, 2, 0);   -- Kim Long (sẽ DQ)

-- Kết quả đã công bố: FinishTime (giây), xếp hạng theo FinalTime; Kim Long DQ
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
-- Giữ nguyên schema. Block này chỉ thêm dữ liệu test rõ ràng cho 5 luồng.
-- Quy ước tài khoản demo: mật khẩu 123456.
-- ID nên lấy bằng SELECT theo Username/RaceName, không hard-code khi demo nếu DB đã có dữ liệu cũ.
-- ============================================================

-- 1) Odds config bổ sung từ file baseodds.txt để BettingService có đủ dữ liệu tính odds.
MERGE SystemConfigs AS target
USING (VALUES
('ODDS_BASE_RANK_1', '1.50', N'Base odds cho ngựa rank 1'),
('ODDS_BASE_RANK_2', '1.65', N'Base odds cho ngựa rank 2'),
('ODDS_BASE_RANK_3', '1.80', N'Base odds cho ngựa rank 3'),
('ODDS_BASE_RANK_4', '2.00', N'Base odds cho ngựa rank 4'),
('ODDS_BASE_RANK_5', '2.20', N'Base odds cho ngựa rank 5'),
('ODDS_BASE_RANK_6', '2.40', N'Base odds cho ngựa rank 6'),
('ODDS_BASE_RANK_7', '2.60', N'Base odds cho ngựa rank 7'),
('ODDS_BASE_RANK_8', '2.80', N'Base odds cho ngựa rank 8'),
('ODDS_BASE_RANK_9', '3.00', N'Base odds cho ngựa rank 9'),
('ODDS_BASE_RANK_10', '3.25', N'Base odds cho ngựa rank 10'),
('ODDS_BASE_RANK_11', '3.50', N'Base odds cho ngựa rank 11'),
('ODDS_BASE_RANK_12', '3.75', N'Base odds cho ngựa rank 12'),
('ODDS_BASE_RANK_13', '4.00', N'Base odds cho ngựa rank 13'),
('ODDS_BASE_RANK_14', '4.25', N'Base odds cho ngựa rank 14'),
('ODDS_BASE_RANK_15', '4.50', N'Base odds cho ngựa rank 15'),
('ODDS_BASE_RANK_OVER_15', '5.00', N'Base odds cho ngựa ngoài top 15'),
('ODDS_BASE_UNRANKED', '2.50', N'Base odds cho ngựa chưa có xếp hạng'),
('EXACT_POSITION_FACTOR', '0.75', N'Hệ số tăng odds khi cược đúng vị trí'),
('ODDS_MAX', '15.00', N'Giới hạn odds tối đa')
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

-- 2) Admin flow: có 1 tài khoản chờ duyệt sạch để test approve/reject.
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
    VALUES (@FlowPendingOwnerUserID, '079205009999', N'Hà Nội', N'FLOW Pending Stable', 'FLOW-OWN-PENDING');
END;

-- 3) Organizer/Admin tournament flow: tạo đủ Draft/Pending/Open để test từng màn.
IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'FLOW_ADMIN_PENDING_TOURNAMENT')
BEGIN
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy)
    VALUES (N'FLOW_ADMIN_PENDING_TOURNAMENT', N'Tournament chờ Admin duyệt.', N'Hà Nội', DATEADD(DAY, 7, CAST(GETDATE() AS DATE)), DATEADD(DAY, 14, CAST(GETDATE() AS DATE)), 30000000, 12, 12, 'PendingApproval', @OrganizerUserID);
END;

IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'FLOW_ORGANIZER_DRAFT_TOURNAMENT')
BEGIN
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy)
    VALUES (N'FLOW_ORGANIZER_DRAFT_TOURNAMENT', N'Tournament draft cho Organizer sửa/gửi Admin.', N'Hồ Chí Minh', DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), DATEADD(DAY, 18, CAST(GETDATE() AS DATE)), 45000000, 16, 16, 'Draft', @OrganizerUserID);
END;

IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'FLOW_TEST_OPEN_TOURNAMENT')
BEGIN
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt)
    VALUES (N'FLOW_TEST_OPEN_TOURNAMENT', N'Tournament mở sẵn cho Owner/Referee/Spectator/Organizer test.', N'Đà Nẵng', DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 20, CAST(GETDATE() AS DATE)), 60000000, 24, 24, 'Open', @OrganizerUserID, @AdminUserID, GETDATE());
END;

DECLARE @FlowTournamentID INT = (SELECT TournamentID FROM Tournaments WHERE TournamentName = N'FLOW_TEST_OPEN_TOURNAMENT');

IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @FlowTournamentID AND RoundName = N'FLOW Round 1')
BEGIN
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@FlowTournamentID, N'FLOW Round 1', 1, CAST(GETDATE() AS DATE), DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), N'Round dùng để test 5 flow.');
END;

DECLARE @FlowRoundID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @FlowTournamentID AND RoundName = N'FLOW Round 1');

-- 4) Thêm ngựa test riêng để không đụng seed cũ.
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H001')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner1ID, N'FLOW Owner Horse Ready', N'Thoroughbred', 2020, N'Đen', N'Male', 450.00, 'FLOW-H001', N'Hoạt động', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H002')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner1ID, N'FLOW Owner Horse Invite', N'Arabian', 2021, N'Nâu', N'Female', 430.00, 'FLOW-H002', N'Hoạt động', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H003')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner2ID, N'FLOW Pending Approval Horse', N'Mustang', 2020, N'Trắng', N'Male', 440.00, 'FLOW-H003', N'Hoạt động', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H004')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner2ID, N'FLOW Betting Horse A', N'Warmblood', 2019, N'Xám', N'Male', 455.00, 'FLOW-H004', N'Hoạt động', 1);
END;
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'FLOW-H005')
BEGIN
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, IsActive)
    VALUES (@Owner1ID, N'FLOW Betting Horse B', N'Andalusian', 2018, N'Vàng', N'Female', 420.00, 'FLOW-H005', N'Hoạt động', 1);
END;

DECLARE @HorseReady INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H001');
DECLARE @HorseInvite INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H002');
DECLARE @HorsePending INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H003');
DECLARE @HorseBetA INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H004');
DECLARE @HorseBetB INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'FLOW-H005');

IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @HorseReady)
BEGIN
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@HorseReady, CAST(GETDATE() AS DATE), N'FLOW Vet', N'Hoạt động', N'Đủ điều kiện thi đấu', N'Demo kiểm tra trước đua', @OrganizerUserID);
END;
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @HorseBetA)
BEGIN
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@HorseBetA, CAST(GETDATE() AS DATE), N'FLOW Vet', N'Hoạt động', N'Đủ điều kiện thi đấu', N'Demo kiểm tra trước đua', @OrganizerUserID);
END;
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @HorsePending)
BEGIN
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@HorsePending, CAST(GETDATE() AS DATE), N'FLOW Vet', N'Hoạt động', N'Đủ điều kiện để Organizer duyệt', N'Demo duyệt đăng ký', @OrganizerUserID);
END;

-- 5) Races tách riêng theo flow để demo không phá nhau.
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'FLOW_OWNER_ENTRY_RACE')
BEGIN
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@FlowTournamentID, @FlowRoundID, N'FLOW_OWNER_ENTRY_RACE', DATEADD(DAY, 4, GETDATE()), 1000, N'Flat', 8, 10000000, 5000000, 2000000, 'RegistrationOpen', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 3, GETDATE()));
END;
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'FLOW_REFEREE_RESULT_RACE')
BEGIN
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@FlowTournamentID, @FlowRoundID, N'FLOW_REFEREE_RESULT_RACE', DATEADD(DAY, 2, GETDATE()), 1200, N'Flat', 8, 15000000, 7000000, 3000000, 'Scheduled', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()));
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

-- 6) Entries cho từng flow.
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

-- 7) Owner/Jockey flow: entry approved nhưng chưa có jockey, có invitation pending.
IF NOT EXISTS (SELECT 1 FROM JockeyInvitations WHERE EntryID = @OwnerEntryID AND JockeyID = @Jockey2ID)
BEGIN
    INSERT INTO JockeyInvitations (EntryID, JockeyID, InvitedByOwner, Message, Status)
    VALUES (@OwnerEntryID, @Jockey2ID, @Owner1ID, N'FLOW: mời jockey2 cầm cương cho Owner Entry Race.', 'Pending');
END;

-- 8) Referee assignment + notification cho race riêng.
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @RefRaceID AND RefereeID = @RefereeID)
BEGIN
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@RefRaceID, @RefereeID, N'Chief');
END;
IF NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefRaceID)
BEGIN
    INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
    VALUES (@RefereeUserID, N'FLOW: Bạn được phân công trọng tài', N'Bạn là trọng tài chính của FLOW_REFEREE_RESULT_RACE.', 'RefereeAssigned', @RefRaceID, 'Race');
END;

-- 9) Betting flow: nạp sẵn ví + đặt sẵn 1 vé Pending để test history, vẫn còn tiền để đặt thêm.
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
    SELECT WalletID, -50000, 'BetPlaced', N'FLOW seed: đặt cược WIN FLOW_BETTING_RACE', 'Bet', @FlowSeedBetID
    FROM Wallets WHERE UserID = @SpectatorUserID;
END;

-- 10) Organizer flow: pending entry + assigned referee list + notification sample.
IF NOT EXISTS (SELECT 1 FROM RaceReferees WHERE RaceID = @OrgRaceID AND RefereeID = @RefereeID)
BEGIN
    INSERT INTO RaceReferees (RaceID, RefereeID, Role) VALUES (@OrgRaceID, @RefereeID, N'Assistant');
END;
IF NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @OrganizerUserID AND NotifType = 'EntryPendingApproval' AND RelatedEntityID = @OrgPendingEntryID)
BEGIN
    INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
    VALUES (@OrganizerUserID, N'FLOW: Có đăng ký chờ duyệt', N'FLOW Pending Approval Horse đang chờ Organizer duyệt.', 'EntryPendingApproval', @OrgPendingEntryID, 'Entry');
END;

-- 11) Leaderboard/stats đủ dữ liệu cho màn xếp hạng và odds rank.
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

-- 12) Extra demo seed: thêm người, ngựa, giải, race, cược và kết quả để UI nhìn đủ dữ liệu hơn.
IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'organizer2')
BEGIN
    INSERT INTO Users (Username, PasswordHash, FullName, Email, Phone, RoleID, IsActive, IsApproved) VALUES
    ('organizer2', @Pwd, N'Ban Tổ Chức B', 'organizer2@gmail.com', '0900000210', 6, 1, 1),
    ('owner3', @Pwd, N'Phan Văn Owner C', 'owner3@gmail.com', '0900000211', 2, 1, 1),
    ('owner4', @Pwd, N'Đỗ Thị Owner D', 'owner4@gmail.com', '0900000212', 2, 1, 1),
    ('jockey3', @Pwd, N'Ngô Văn Jockey E', 'jockey3@gmail.com', '0900000213', 3, 1, 1),
    ('jockey4', @Pwd, N'Bùi Văn Jockey F', 'jockey4@gmail.com', '0900000214', 3, 1, 1),
    ('referee2', @Pwd, N'Trần Văn Referee F', 'referee2@gmail.com', '0900000215', 4, 1, 1),
    ('spectator2', @Pwd, N'Đặng Văn Spectator B', 'spectator2@gmail.com', '0900000216', 5, 1, 1),
    ('spectator3', @Pwd, N'Vũ Thị Spectator C', 'spectator3@gmail.com', '0900000217', 5, 1, 1);
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
    VALUES (@Owner3UserID, '079205002011', N'Cần Thơ', N'Mekong Stable', 'OWN003');
IF NOT EXISTS (SELECT 1 FROM HorseOwners WHERE UserID = @Owner4UserID)
    INSERT INTO HorseOwners (UserID, NationalID, Address, Organization, LicenseNumber)
    VALUES (@Owner4UserID, '079205002012', N'Hải Phòng', N'Ocean Stable', 'OWN004');
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
    VALUES (@Owner3ID, N'Sao Băng', N'Thoroughbred', 2020, N'Đen trắng', N'Male', 448.00, 'DM-H001', N'Hoạt động', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H002')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner3ID, N'Ngân Hà', N'Arabian', 2021, N'Xám bạc', N'Female', 426.00, 'DM-H002', N'Hoạt động', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H003')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner4ID, N'Phong Vân', N'Mustang', 2019, N'Nâu sẫm', N'Male', 452.00, 'DM-H003', N'Hoạt động', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H004')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner4ID, N'Lam Sơn', N'Warmblood', 2018, N'Vàng nâu', N'Male', 463.00, 'DM-H004', N'Hoạt động', @OrganizerUserID, GETDATE(), 1);
IF NOT EXISTS (SELECT 1 FROM Horses WHERE RegisterCode = 'DM-H005')
    INSERT INTO Horses (OwnerID, HorseName, Breed, BirthYear, Color, Gender, WeightKg, RegisterCode, HealthStatus, HealthUpdatedBy, HealthUpdatedAt, IsActive)
    VALUES (@Owner3ID, N'Hồng Nhật', N'Andalusian', 2020, N'Đỏ nâu', N'Female', 432.00, 'DM-H005', N'Bị thương nhẹ', @OrganizerUserID, GETDATE(), 1);

DECLARE @Dmh1 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H001');
DECLARE @Dmh2 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H002');
DECLARE @Dmh3 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H003');
DECLARE @Dmh4 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H004');
DECLARE @Dmh5 INT = (SELECT HorseID FROM Horses WHERE RegisterCode = 'DM-H005');

IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @Dmh1)
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@Dmh1, CAST(GETDATE() AS DATE), N'BS Nguyễn Minh', N'Hoạt động', N'Đủ điều kiện thi đấu', N'Seed demo mở rộng', @OrganizerUserID);
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @Dmh3)
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@Dmh3, CAST(GETDATE() AS DATE), N'BS Nguyễn Minh', N'Hoạt động', N'Đủ điều kiện thi đấu', N'Seed demo mở rộng', @OrganizerUserID);
IF NOT EXISTS (SELECT 1 FROM HorseHealthRecords WHERE HorseID = @Dmh5)
    INSERT INTO HorseHealthRecords (HorseID, CheckDate, VetName, HealthStatus, Diagnosis, Notes, RecordedBy)
    VALUES (@Dmh5, CAST(GETDATE() AS DATE), N'BS Nguyễn Minh', N'Bị thương nhẹ', N'Cần theo dõi thêm trước khi duyệt', N'Dữ liệu demo trạng thái sức khỏe', @OrganizerUserID);

IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'DEMO_SHOWCASE_CUP_2026')
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt)
    VALUES (N'DEMO_SHOWCASE_CUP_2026', N'Giải showcase nhiều user/ngựa để demo UI.', N'Hà Nội', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 12, CAST(GETDATE() AS DATE)), 80000000, 32, 32, 'Open', @OrganizerUserID, @AdminUserID, GETDATE());

DECLARE @DemoTournamentID INT = (SELECT TournamentID FROM Tournaments WHERE TournamentName = N'DEMO_SHOWCASE_CUP_2026');
IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Vòng loại')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@DemoTournamentID, N'DEMO Vòng loại', 1, DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 5, CAST(GETDATE() AS DATE)), N'Vòng loại showcase');
IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Chung kết')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@DemoTournamentID, N'DEMO Chung kết', 2, DATEADD(DAY, 8, CAST(GETDATE() AS DATE)), DATEADD(DAY, 12, CAST(GETDATE() AS DATE)), N'Chung kết showcase');

DECLARE @DemoRound1ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Vòng loại');
DECLARE @DemoRound2ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @DemoTournamentID AND RoundName = N'DEMO Chung kết');

IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'DEMO_SHOWCASE_OPEN_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@DemoTournamentID, @DemoRound1ID, N'DEMO_SHOWCASE_OPEN_RACE', DATEADD(DAY, 3, GETDATE()), 1000, N'Flat', 10, 18000000, 9000000, 4000000, 'RegistrationOpen', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 2, GETDATE()));
IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'DEMO_SHOWCASE_REFEREE_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@DemoTournamentID, @DemoRound1ID, N'DEMO_SHOWCASE_REFEREE_RACE', DATEADD(DAY, 2, GETDATE()), 1300, N'Flat', 10, 22000000, 11000000, 5000000, 'Scheduled', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()));
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
    VALUES (@DemoOpenEntry2, @Jockey4ID, @Owner3ID, N'DEMO: mời jockey4 thi đấu cùng Ngân Hà.', 'Pending');

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
    VALUES (@DemoFinishedRaceID, @DemoFinalEntry2, @Referee2ID, 'LanLane', 5.00, 0, N'demo-evidence/lan-lane.jpg', N'Lấn lane ở đoạn cua cuối, cộng 5 giây.');
IF NOT EXISTS (SELECT 1 FROM Violations WHERE RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry3 AND ViolationType = 'CanDuong')
    INSERT INTO Violations (RaceID, EntryID, RefereeID, ViolationType, PenaltySeconds, IsDQ, EvidenceImageURL, Description)
    VALUES (@DemoFinishedRaceID, @DemoFinalEntry3, @Referee2ID, 'CanDuong', 10.00, 0, N'demo-evidence/can-duong.jpg', N'Cản đường đối thủ, cộng 10 giây.');

EXEC sp_UpdateRaceResultRanking @DemoFinishedRaceID;

IF NOT EXISTS (SELECT 1 FROM RaceMinutes WHERE RaceID = @DemoFinishedRaceID)
    INSERT INTO RaceMinutes (RaceID, RefereeID, Content, PreRaceChecks, PostRaceNotes, MinutesFileURL, SentToOwners, SentAt)
    VALUES (@DemoFinishedRaceID, @Referee2ID, N'Demo biên bản đã lập và gửi owner.', N'Đã kiểm tra đủ ngựa, jockey, làn đường.', N'Có 2 vi phạm đã ghi nhận.', N'demo-uploads/showcase-minutes.webp', 1, GETDATE());

IF NOT EXISTS (SELECT 1 FROM Bets WHERE UserID = @Spectator2UserID AND RaceID = @DemoOpenRaceID AND EntryID = @DemoOpenEntry1 AND BetType = 'WIN')
BEGIN
    INSERT INTO Bets (UserID, RaceID, EntryID, BetType, Amount, Odds, PotentialPayout, Status)
    VALUES (@Spectator2UserID, @DemoOpenRaceID, @DemoOpenEntry1, 'WIN', 100000, 1.70, 170000, 'Pending');
    DECLARE @DemoBet1 INT = SCOPE_IDENTITY();
    INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
    SELECT WalletID, -100000, 'BetPlaced', N'DEMO: đặt cược Sao Băng thắng', 'Bet', @DemoBet1
    FROM Wallets WHERE UserID = @Spectator2UserID;
END;
IF NOT EXISTS (SELECT 1 FROM Bets WHERE UserID = @Spectator3UserID AND RaceID = @DemoFinishedRaceID AND EntryID = @DemoFinalEntry1 AND BetType = 'WIN')
BEGIN
    INSERT INTO Bets (UserID, RaceID, EntryID, BetType, Amount, Odds, PotentialPayout, Status, SettledAt)
    VALUES (@Spectator3UserID, @DemoFinishedRaceID, @DemoFinalEntry1, 'WIN', 80000, 1.55, 124000, 'Won', GETDATE());
    DECLARE @DemoBet2 INT = SCOPE_IDENTITY();
    INSERT INTO WalletTransactions (WalletID, Amount, TransactionType, Description, RelatedEntity, RelatedEntityID)
    SELECT WalletID, 124000, 'BetWon', N'DEMO: thắng cược race đã kết thúc', 'Bet', @DemoBet2
    FROM Wallets WHERE UserID = @Spectator3UserID;
END;

INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @Referee2UserID, N'DEMO: Bạn được phân công trọng tài', N'Bạn là trọng tài chính của DEMO_SHOWCASE_FINISHED_RACE.', 'RefereeAssigned', @DemoFinishedRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @Referee2UserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @DemoFinishedRaceID);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @OrganizerUserID, N'DEMO: Có đăng ký cần kiểm tra sức khỏe', N'Hồng Nhật đang chờ duyệt, trạng thái sức khỏe cần theo dõi.', 'EntryPendingApproval', @DemoOpenPendingEntry, 'Entry'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @OrganizerUserID AND NotifType = 'EntryPendingApproval' AND RelatedEntityID = @DemoOpenPendingEntry);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @Owner3UserID, N'DEMO: Biên bản đã gửi', N'Biên bản DEMO_SHOWCASE_FINISHED_RACE đã được gửi tới owner.', 'RaceMinutesSent', @DemoFinishedRaceID, 'Race'
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

-- 13) Referee1 demo pack: dữ liệu riêng để referee1 review trọn luồng.
IF NOT EXISTS (SELECT 1 FROM Tournaments WHERE TournamentName = N'REFEREE1_DEMO_CUP_2026')
    INSERT INTO Tournaments (TournamentName, Description, Location, StartDate, EndDate, BudgetTotal, MaxHorses, MaxParticipants, Status, CreatedBy, ApprovedByAdmin, ApprovedAt)
    VALUES (N'REFEREE1_DEMO_CUP_2026',
            N'Giải riêng cho referee1 demo: nhận phân công, kiểm tra trước đua, nhập kết quả, ghi vi phạm, lập biên bản.',
            N'TP. Hồ Chí Minh',
            DATEADD(DAY, -2, CAST(GETDATE() AS DATE)),
            DATEADD(DAY, 10, CAST(GETDATE() AS DATE)),
            90000000, 30, 30, 'Open', @OrganizerUserID, @AdminUserID, GETDATE());

DECLARE @RefDemoTournamentID INT = (SELECT TournamentID FROM Tournaments WHERE TournamentName = N'REFEREE1_DEMO_CUP_2026');

IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Vòng loại')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@RefDemoTournamentID, N'Referee1 Vòng loại', 1, DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 4, CAST(GETDATE() AS DATE)), N'Round để referee1 kiểm tra trước đua và nhập dữ liệu.');
IF NOT EXISTS (SELECT 1 FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Chung kết')
    INSERT INTO Rounds (TournamentID, RoundName, RoundOrder, StartDate, EndDate, Description)
    VALUES (@RefDemoTournamentID, N'Referee1 Chung kết', 2, DATEADD(DAY, 5, CAST(GETDATE() AS DATE)), DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), N'Round để referee1 xem race đã có kết quả và biên bản.');

DECLARE @RefDemoRound1ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Vòng loại');
DECLARE @RefDemoRound2ID INT = (SELECT RoundID FROM Rounds WHERE TournamentID = @RefDemoTournamentID AND RoundName = N'Referee1 Chung kết');

IF NOT EXISTS (SELECT 1 FROM Races WHERE RaceName = N'REF1_PRECHECK_RACE')
    INSERT INTO Races (TournamentID, RoundID, RaceName, RaceDate, TrackLength, TrackType, MaxParticipants, PrizeFirst, PrizeSecond, PrizeThird, Status, RegistrationOpen, RegistrationClose)
    VALUES (@RefDemoTournamentID, @RefDemoRound1ID, N'REF1_PRECHECK_RACE', DATEADD(DAY, 1, GETDATE()), 1000, N'Flat', 8, 15000000, 8000000, 3000000, 'Scheduled', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()));
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
    VALUES (@RefOngoingRaceID, 'Scheduled', 'Ongoing', @RefereeUserID);
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
    VALUES (@RefFinishedRaceID, @RefFinishedEntry2, @RefereeID, 'XuatPhatSai', 3.00, 0, N'demo-evidence/ref1-xuat-phat-sai.jpg', N'Xuất phát sớm, cộng 3 giây.');

EXEC sp_UpdateRaceResultRanking @RefFinishedRaceID;

IF NOT EXISTS (SELECT 1 FROM RaceMinutes WHERE RaceID = @RefFinishedRaceID)
    INSERT INTO RaceMinutes (RaceID, RefereeID, Content, PreRaceChecks, PostRaceNotes, MinutesFileURL, SentToOwners, SentAt)
    VALUES (@RefFinishedRaceID, @RefereeID,
            N'Referee1 demo: race đã kết thúc, kết quả đã được Organizer duyệt nhưng chưa publish.',
            N'Đã kiểm tra danh sách ngựa, jockey, làn xuất phát và sức khỏe trước đua.',
            N'Có 1 vi phạm xuất phát sai, hệ thống đã cộng 3 giây và xếp hạng lại.',
            N'demo-uploads/referee1-minutes.webp', 1, GETDATE());

INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @RefereeUserID, N'REF1: Phân công race kiểm tra trước đua', N'Bạn là trọng tài chính của REF1_PRECHECK_RACE.', 'RefereeAssigned', @RefPreRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefPreRaceID);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @RefereeUserID, N'REF1: Phân công race đang diễn ra', N'Bạn cần nhập kết quả và ghi vi phạm cho REF1_ONGOING_INPUT_RACE.', 'RefereeAssigned', @RefOngoingRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefOngoingRaceID);
INSERT INTO Notifications (UserID, Title, Body, NotifType, RelatedEntityID, RelatedEntity)
SELECT @RefereeUserID, N'REF1: Race đã có biên bản', N'REF1_FINISHED_MINUTES_RACE đã có kết quả, vi phạm và biên bản để demo.', 'RefereeAssigned', @RefFinishedRaceID, 'Race'
WHERE NOT EXISTS (SELECT 1 FROM Notifications WHERE UserID = @RefereeUserID AND NotifType = 'RefereeAssigned' AND RelatedEntityID = @RefFinishedRaceID);

-- 12) Demo helper: in ra ID cần copy vào Postman variables.
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
PRINT N' HorseRacingDB v2 tạo thành công!';
PRINT N' Đã đồng bộ Business Flow v3 + fix bug (settle@publish, DQ, prize→ví).';
PRINT N'============================================================';
GO




