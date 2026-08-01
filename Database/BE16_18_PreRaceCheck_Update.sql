USE HorseRacingDB;
GO

IF OBJECT_ID('PreRaceChecks', 'U') IS NULL
BEGIN
    CREATE TABLE PreRaceChecks (
        PreRaceCheckID INT IDENTITY(1,1) PRIMARY KEY,
        RaceID         INT          NOT NULL REFERENCES Races(RaceID),
        EntryID        INT          NOT NULL REFERENCES RaceEntries(EntryID),
        HorseID        INT          NOT NULL REFERENCES Horses(HorseID),
        RefereeID      INT          NOT NULL REFERENCES Referees(RefereeID),
        Status         NVARCHAR(20) NOT NULL DEFAULT 'Pending',
        Reason         NVARCHAR(500),
        CheckedAt      DATETIME2,
        CONSTRAINT UQ_PreRaceChecks_Race_Entry UNIQUE (RaceID, EntryID),
        CONSTRAINT CK_PreRaceChecks_Status CHECK (Status IN ('Pending', 'Checked', 'Rejected'))
    );
END
GO

INSERT INTO PreRaceChecks (RaceID, EntryID, HorseID, RefereeID, Status)
SELECT re.RaceID, re.EntryID, re.HorseID, rr.RefereeID, 'Pending'
FROM RaceEntries re
JOIN (
    SELECT RaceID, MIN(RefereeID) AS RefereeID
    FROM RaceReferees
    GROUP BY RaceID
) rr ON rr.RaceID = re.RaceID
WHERE re.RegistrationStatus IN ('Approved', 'Ready')
  AND NOT EXISTS (
      SELECT 1
      FROM PreRaceChecks prc
      WHERE prc.RaceID = re.RaceID
        AND prc.EntryID = re.EntryID
  );
GO
