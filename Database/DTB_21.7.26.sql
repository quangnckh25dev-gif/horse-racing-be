/*
    Migration: Add DepositRequests table for manual wallet deposit flow
    Project: Horse Racing

    Purpose:
    - Users create deposit requests by BANK or MOMO.
    - Wallet balance is increased only after Admin approves the request.
    - Existing Wallets and WalletTransactions tables are not modified or removed.

    Target database:
    - HorseRacingDB

    How to run:
    1. Open SQL Server Management Studio / Azure Data Studio.
    2. Select database HorseRacingDB.
    3. Run this script.
*/

USE HorseRacingDB;
GO

IF OBJECT_ID(N'dbo.DepositRequests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DepositRequests (
        DepositRequestID INT IDENTITY(1,1) PRIMARY KEY,

        UserID           INT           NOT NULL,
        WalletID         INT           NOT NULL,
        Amount           DECIMAL(18,2) NOT NULL,
        PaymentMethod    NVARCHAR(20)  NOT NULL,
        TransferCode     NVARCHAR(50)  NOT NULL,
        QrCodeUrl        NVARCHAR(500) NULL,
        Status           NVARCHAR(20)  NOT NULL DEFAULT 'Pending',
        AdminNote        NVARCHAR(500) NULL,
        ApprovedBy       INT           NULL,
        ApprovedAt       DATETIME2     NULL,
        CreatedAt        DATETIME2     NOT NULL DEFAULT GETDATE(),
        UpdatedAt        DATETIME2     NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_DepositRequests_Users
            FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),

        CONSTRAINT FK_DepositRequests_Wallets
            FOREIGN KEY (WalletID) REFERENCES dbo.Wallets(WalletID),

        CONSTRAINT FK_DepositRequests_ApprovedBy_Users
            FOREIGN KEY (ApprovedBy) REFERENCES dbo.Users(UserID),

        CONSTRAINT CK_DepositRequests_Amount_Positive
            CHECK (Amount > 0),

        CONSTRAINT CK_DepositRequests_PaymentMethod
            CHECK (PaymentMethod IN ('BANK', 'MOMO')),

        CONSTRAINT CK_DepositRequests_Status
            CHECK (Status IN ('Pending', 'Approved', 'Rejected')),

        CONSTRAINT UQ_DepositRequests_TransferCode
            UNIQUE (TransferCode)
    );

    CREATE INDEX IX_DepositRequests_UserID_CreatedAt
        ON dbo.DepositRequests (UserID, CreatedAt DESC);

    CREATE INDEX IX_DepositRequests_Status_CreatedAt
        ON dbo.DepositRequests (Status, CreatedAt DESC);

    PRINT 'Created dbo.DepositRequests successfully.';
END
ELSE
BEGIN
    PRINT 'dbo.DepositRequests already exists. No changes were made.';
END
GO

SELECT
    TABLE_SCHEMA,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'dbo'
  AND TABLE_NAME = 'DepositRequests';
GO
