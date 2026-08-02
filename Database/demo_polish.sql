/* ============================================================
   DEMO POLISH — làm đẹp dữ liệu để demo/bảo vệ đồ án
   ------------------------------------------------------------
   CÁCH DÙNG (BẮT BUỘC thêm -f 65001 để sqlcmd đọc UTF-8, tránh lỗi font tiếng Việt):
     1) Import seed goc:   sqlcmd -S localhost,1433 -U sa -P 12345 -C -f 65001 -i 2.8.26_DTB.sql
     2) Chay file nay:     sqlcmd -S localhost,1433 -U sa -P 12345 -C -f 65001 -i demo_polish.sql
   (Neu quen -f 65001, chu tieng Viet se bi loi kieu "Giai Dua" -> "Giáº£i Äua".)
   KẾT QUẢ:
     - Tên giải / race / ngựa: tiếng Việt, gọn đẹp
     - Toàn bộ ví = 0đ (dọn sạch giao dịch/nạp/rút/cược/khiếu nại)
     - Jockey có thành tích (Races/Wins) để profile hiển thị
     - Bỏ trạng thái PendingApproval (giải chuyển Draft/Open)
   ============================================================ */
USE HorseRacingDB;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET NOCOUNT ON;

/* ===== 1. Đổi tên giải đấu (tên đẹp, tiếng Việt) ===== */
UPDATE Tournaments SET TournamentName = N'Giải Đua Ngựa Mùa Hè 2026'                  WHERE TournamentID = 1;
UPDATE Tournaments SET TournamentName = N'Giải Đua Ngựa Mùa Thu 2026', Status='Open'  WHERE TournamentID = 2;
UPDATE Tournaments SET TournamentName = N'Giải Đua Ngựa Cúp Quốc Gia', Status='Draft' WHERE TournamentID = 3;
UPDATE Tournaments SET TournamentName = N'Giải Đua Ngựa Mùa Xuân 2026'                WHERE TournamentID = 4;
UPDATE Tournaments SET TournamentName = N'Giải Đua Ngựa Giao Hữu 2026'                WHERE TournamentID = 5;
UPDATE Tournaments SET TournamentName = N'Giải Đua Ngựa Toàn Quốc 2026'               WHERE TournamentID = 6;
UPDATE Tournaments SET TournamentName = N'Giải Đua Ngựa Vô Địch 2026'                 WHERE TournamentID = 7;

/* ===== 2. Đổi tên race ===== */
UPDATE Races SET RaceName = N'Vòng Loại - Lượt Mở Màn'   WHERE RaceID = 1;
UPDATE Races SET RaceName = N'Vòng Loại - Nước Rút 800m'  WHERE RaceID = 2;
UPDATE Races SET RaceName = N'Chung Kết Mùa Hè'           WHERE RaceID = 3;
UPDATE Races SET RaceName = N'Vòng Loại - Đăng Ký'        WHERE RaceID = 4;
UPDATE Races SET RaceName = N'Vòng Loại - Chấm Điểm'      WHERE RaceID = 5;
UPDATE Races SET RaceName = N'Vòng Loại - Cá Cược'        WHERE RaceID = 6;
UPDATE Races SET RaceName = N'Vòng Loại - Duyệt Kết Quả'  WHERE RaceID = 7;
UPDATE Races SET RaceName = N'Vòng Loại - Bảng A'         WHERE RaceID = 8;
UPDATE Races SET RaceName = N'Vòng Loại - Bảng B'         WHERE RaceID = 9;
UPDATE Races SET RaceName = N'Chung Kết Toàn Quốc'        WHERE RaceID = 10;
UPDATE Races SET RaceName = N'Vòng Loại - Kiểm Tra'       WHERE RaceID = 11;
UPDATE Races SET RaceName = N'Vòng Loại - Đang Diễn Ra'   WHERE RaceID = 12;
UPDATE Races SET RaceName = N'Chung Kết Vô Địch'          WHERE RaceID = 13;

/* ===== 3. Đổi tên ngựa "FLOW..." thành tên đẹp ===== */
UPDATE Horses SET HorseName = N'Thần Tốc'  WHERE HorseID = 7;
UPDATE Horses SET HorseName = N'Đại Bàng'  WHERE HorseID = 8;
UPDATE Horses SET HorseName = N'Vô Ảnh'    WHERE HorseID = 9;
UPDATE Horses SET HorseName = N'Lôi Phong' WHERE HorseID = 10;
UPDATE Horses SET HorseName = N'Hỏa Tiễn'  WHERE HorseID = 11;

/* ===== 4. Cho toàn bộ TIỀN = 0 (dọn sạch để demo lại từ đầu) ===== */
DELETE FROM BetSelections;
DELETE FROM BetTickets;
DELETE FROM Bets;
DELETE FROM WalletTransactions;
DELETE FROM DepositComplaints;
DELETE FROM WithdrawalRequests;
DELETE FROM DepositRequests;
UPDATE Wallets SET Balance = 0;

/* ===== 5. Thành tích cho Jockey (profile hiển thị stats) ===== */
UPDATE Jockeys SET TotalRaces = 12, TotalWins = 7 WHERE JockeyID = 1;
UPDATE Jockeys SET TotalRaces = 9,  TotalWins = 3 WHERE JockeyID = 2;
UPDATE Jockeys SET TotalRaces = 15, TotalWins = 9 WHERE JockeyID = 3;
UPDATE Jockeys SET TotalRaces = 6,  TotalWins = 2 WHERE JockeyID = 4;

/* ===== 6. Tài khoản chờ duyệt đúng chuẩn (isActive=1 để hiện ở trang Approve) =====
   Luong dang ky that: user moi = IsActive=1, IsApproved=0. Query pending yeu cau IsActive=1. */
UPDATE Users SET IsApproved = 0, IsActive = 1 WHERE Username IN ('owner_pending', 'flow_pending_owner');

SELECT 'DEMO POLISH DONE' AS Result;
