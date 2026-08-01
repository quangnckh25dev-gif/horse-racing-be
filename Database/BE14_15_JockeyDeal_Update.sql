USE HorseRacingDB;
GO

IF COL_LENGTH('JockeyInvitations', 'DealAmount') IS NULL
BEGIN
    ALTER TABLE JockeyInvitations
    ADD DealAmount DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_JockeyInvitations_DealAmount DEFAULT 1000000;
END
GO

IF COL_LENGTH('JockeyInvitations', 'ResponseReason') IS NULL
BEGIN
    ALTER TABLE JockeyInvitations
    ADD ResponseReason NVARCHAR(500) NULL;
END
GO

UPDATE JockeyInvitations
SET DealAmount = 1000000
WHERE DealAmount IS NULL OR DealAmount <= 0;
GO
