-- Create Database (Optional - Uncomment if needed)
-- CREATE DATABASE CinemaManagement;
-- GO
-- USE CinemaManagement;
-- GO


CREATE TABLE [Role] (
    [role_id] INT IDENTITY(1,1) PRIMARY KEY,
    [name] VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE [Branch] (
    [branch_id] INT IDENTITY(1,1) PRIMARY KEY,
    [name] VARCHAR(100) NOT NULL,
    [location] VARCHAR(255) NOT NULL
);

CREATE TABLE [Movie] (
    [movie_id] INT IDENTITY(1,1) PRIMARY KEY,
    [title] VARCHAR(255) NOT NULL,
    [duration] INT NOT NULL,
    [description] TEXT,
    [release_date] DATE
);

CREATE TABLE [Genre] (
    [genre_id] INT IDENTITY(1,1) PRIMARY KEY,
    [name] VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE [Promotion] (
    [promotion_id] INT IDENTITY(1,1) PRIMARY KEY,
    [code] VARCHAR(50) UNIQUE NOT NULL,
    [discount_type] VARCHAR(20) NOT NULL,
    [discount_value] INT NOT NULL,
    [start_date] DATE,
    [end_date] DATE,
    [usage_limit] INT,
    [status] VARCHAR(20)
);

CREATE TABLE [Membership_Plan] (
    [plan_id] INT IDENTITY(1,1) PRIMARY KEY,
    [name] VARCHAR(100) NOT NULL,
    [discount_percent] DECIMAL(5,2) NOT NULL,
    [price] INT NOT NULL,
    [duration_days] INT NOT NULL
);

CREATE TABLE [Food_Item] (
    [food_id] INT IDENTITY(1,1) PRIMARY KEY,
    [name] VARCHAR(100) NOT NULL,
    [price] INT NOT NULL,
    [category] VARCHAR(50),
    [status] VARCHAR(20)
);


CREATE TABLE [User] (
    [user_id] INT IDENTITY(1,1) PRIMARY KEY,
    [name] VARCHAR(100) NOT NULL,
    [email] VARCHAR(100) UNIQUE NOT NULL,
    [username] VARCHAR(50) UNIQUE NOT NULL,
    [password] VARCHAR(255) NOT NULL,
    [phone] VARCHAR(20),
    [role_id] INT FOREIGN KEY REFERENCES [Role]([role_id]),
    [status] VARCHAR(20) DEFAULT 'Active',
    [last_login] DATETIME,
    [created_at] DATETIME DEFAULT GETDATE()
);

CREATE TABLE [Hall] (
    [hall_id] INT IDENTITY(1,1) PRIMARY KEY,
    [branch_id] INT FOREIGN KEY REFERENCES [Branch]([branch_id]),
    [name] VARCHAR(50) NOT NULL
);

CREATE TABLE [Movie_Genre] (
    [movie_id] INT FOREIGN KEY REFERENCES [Movie]([movie_id]),
    [genre_id] INT FOREIGN KEY REFERENCES [Genre]([genre_id]),
    PRIMARY KEY ([movie_id], [genre_id])
);


CREATE TABLE [Seat] (
    [seat_id] INT IDENTITY(1,1) PRIMARY KEY,
    [hall_id] INT FOREIGN KEY REFERENCES [Hall]([hall_id]),
    [seat_row] VARCHAR(5) NOT NULL,
    [seat_number] INT NOT NULL,
    [type] VARCHAR(20)
);

CREATE TABLE [Showtime] (
    [showtime_id] INT IDENTITY(1,1) PRIMARY KEY,
    [movie_id] INT FOREIGN KEY REFERENCES [Movie]([movie_id]),
    [hall_id] INT FOREIGN KEY REFERENCES [Hall]([hall_id]),
    [start_time] DATETIME NOT NULL,
    [end_time] DATETIME NOT NULL,
    [price] INT NOT NULL
);

CREATE TABLE [User_Membership] (
    [user_membership_id] INT IDENTITY(1,1) PRIMARY KEY,
    [user_id] INT FOREIGN KEY REFERENCES [User]([user_id]),
    [plan_id] INT FOREIGN KEY REFERENCES [Membership_Plan]([plan_id]),
    [start_date] DATE NOT NULL,
    [end_date] DATE NOT NULL,
    [status] VARCHAR(20)
);

CREATE TABLE [Loyalty_Point] (
    [point_id] INT IDENTITY(1,1) PRIMARY KEY,
    [user_id] INT FOREIGN KEY REFERENCES [User]([user_id]),
    [points] INT NOT NULL,
    [type] VARCHAR(20),
    [created_at] DATETIME DEFAULT GETDATE()
);

CREATE TABLE [Review] (
    [review_id] INT IDENTITY(1,1) PRIMARY KEY,
    [user_id] INT FOREIGN KEY REFERENCES [User]([user_id]),
    [movie_id] INT FOREIGN KEY REFERENCES [Movie]([movie_id]),
    [rating] INT CHECK ([rating] BETWEEN 1 AND 5),
    [comment] TEXT,
    [created_at] DATETIME DEFAULT GETDATE()
);

CREATE TABLE [Notification] (
    [notification_id] INT IDENTITY(1,1) PRIMARY KEY,
    [user_id] INT FOREIGN KEY REFERENCES [User]([user_id]),
    [title] VARCHAR(255) NOT NULL,
    [message] TEXT,
    [type] VARCHAR(50),
    [is_read] BIT DEFAULT 0, -- MS-SQL uses BIT for boolean
    [created_at] DATETIME DEFAULT GETDATE()
);

CREATE TABLE [Audit_Log] (
    [log_id] INT IDENTITY(1,1) PRIMARY KEY,
    [user_id] INT FOREIGN KEY REFERENCES [User]([user_id]),
    [entity_type] VARCHAR(50) NOT NULL,
    [entity_id] INT,
    [action] VARCHAR(50) NOT NULL,
    [details] TEXT,
    [created_at] DATETIME DEFAULT GETDATE()
);


CREATE TABLE [Booking] (
    [booking_id] INT IDENTITY(1,1) PRIMARY KEY,
    [user_id] INT FOREIGN KEY REFERENCES [User]([user_id]),
    [showtime_id] INT FOREIGN KEY REFERENCES [Showtime]([showtime_id]),
    [promotion_id] INT FOREIGN KEY REFERENCES [Promotion]([promotion_id]),
    [total_price] BIGINT NOT NULL,
    [status] VARCHAR(20),
    [created_at] DATETIME DEFAULT GETDATE()
);

CREATE TABLE [Ticket] (
    [ticket_id] INT IDENTITY(1,1) PRIMARY KEY,
    [booking_id] INT FOREIGN KEY REFERENCES [Booking]([booking_id]),
    [seat_id] INT FOREIGN KEY REFERENCES [Seat]([seat_id]),
    [qr_code] VARCHAR(255) UNIQUE,
    [status] VARCHAR(20)
);

CREATE TABLE [Seat_Reservation] (
    [reservation_id] INT IDENTITY(1,1) PRIMARY KEY,
    [showtime_id] INT FOREIGN KEY REFERENCES [Showtime]([showtime_id]),
    [seat_id] INT FOREIGN KEY REFERENCES [Seat]([seat_id]),
    [status] VARCHAR(20),
    [expired_at] DATETIME NOT NULL
);

CREATE TABLE [Payment] (
    [payment_id] INT IDENTITY(1,1) PRIMARY KEY,
    -- Booking ||--|| Payment (1-1 relationship) -> Added UNIQUE constraint
    [booking_id] INT UNIQUE FOREIGN KEY REFERENCES [Booking]([booking_id]),
    [amount] BIGINT NOT NULL,
    [method] VARCHAR(20) NOT NULL,
    [status] VARCHAR(20),
    [created_at] DATETIME DEFAULT GETDATE()
);

CREATE TABLE [Promotion_Usage] (
    [usage_id] INT IDENTITY(1,1) PRIMARY KEY,
    [promotion_id] INT FOREIGN KEY REFERENCES [Promotion]([promotion_id]),
    [user_id] INT FOREIGN KEY REFERENCES [User]([user_id]),
    [booking_id] INT FOREIGN KEY REFERENCES [Booking]([booking_id]),
    [used_at] DATETIME DEFAULT GETDATE()
);


CREATE TABLE [Food_Order] (
    [food_order_id] INT IDENTITY(1,1) PRIMARY KEY,
    [booking_id] INT FOREIGN KEY REFERENCES [Booking]([booking_id]),
    [total_price] BIGINT NOT NULL,
    [status] VARCHAR(20),
    [created_at] DATETIME DEFAULT GETDATE()
);

CREATE TABLE [Food_Order_Item] (
    [id] INT IDENTITY(1,1) PRIMARY KEY,
    [food_order_id] INT FOREIGN KEY REFERENCES [Food_Order]([food_order_id]),
    [food_id] INT FOREIGN KEY REFERENCES [Food_Item]([food_id]),
    [quantity] INT NOT NULL,
    [price] INT NOT NULL
);
GO
-- ===================================================
-- MySQL Script - Multi-Branch Cinema Management
-- Phiên bản: Tối ưu JOIN, Phân quyền Chi nhánh & Quản lý Nhân viên (Manager - Staff)
-- Khởi tạo: Xóa và làm mới toàn bộ Database tự động
-- ===================================================
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP DATABASE IF EXISTS `cinema_management_db`;
CREATE DATABASE `cinema_management_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `cinema_management_db`;

SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================
-- 1. PHÂN HỆ: CHI NHÁNH
-- ===================================================

CREATE TABLE `Branch` (
    `branch_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `address` VARCHAR(255) NOT NULL,
    `city` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `status` VARCHAR(50) NOT NULL, -- Active, Inactive, Maintenance
    `opening_time` TIME NOT NULL,
    `closing_time` TIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================================================
-- 2. PHÂN HỆ: USER, AUTHENTICATION & QUẢN LÝ NHÂN VIÊN (ĐÃ CẬP NHẬT)
-- ===================================================

CREATE TABLE `Role` (
    `role_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL -- Admin, Manager, Staff, Customer
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `Role` (`role_id`, `name`) VALUES
    (1, 'Admin'),
    (2, 'Manager'),
    (3, 'Staff'),
    (4, 'Customer');

CREATE TABLE `User` (
    `user_id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(150) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `phone` VARCHAR(20) NOT NULL,
    `role_id` INT NOT NULL,
    -- Giúp liên kết nhân viên/quản lý với 1 chi nhánh nhất định. Khách hàng để NULL.
    `branch_id` INT NULL, 
    `manager_id` INT NULL, 
    `status` VARCHAR(50) NOT NULL, -- Active, Inactive, Banned
    `email_verified` TINYINT(1) NOT NULL DEFAULT 0,
    `forgot_password_code` VARCHAR(20) NULL,
    `forgot_password_code_expire` DATETIME NULL,
    `active_code` VARCHAR(20) NULL,
    `active_code_expire` DATETIME NULL,
    `avatar_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`role_id`) REFERENCES `Role`(`role_id`),
    FOREIGN KEY (`branch_id`) REFERENCES `Branch`(`branch_id`),
    FOREIGN KEY (`manager_id`) REFERENCES `User`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================================================
-- 3. PHÂN HỆ ĐỘC LẬP: AUDIT LOG, LOYALTY POINT & MEMBERSHIP
-- ===================================================

CREATE TABLE `AuditLog` (
    `log_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `entity_type` VARCHAR(100) NOT NULL,
    `entity_id` INT NOT NULL,            
    `action` VARCHAR(50) NOT NULL,       
    `details` TEXT NOT NULL,             
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `LoyaltyPoint` (
    `point_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `points` INT NOT NULL,                
    `type` VARCHAR(100) NOT NULL,        -- Earned, Redeemed
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `UserMembership` (
    `user_membership_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `plan_name` VARCHAR(100) NOT NULL,       
    `discount_percent` INT NOT NULL DEFAULT 0, 
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `status` VARCHAR(50) NOT NULL,            
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================================================
-- 4. PHÂN HỆ: PHIM & LỊCH CHIẾU
-- ===================================================

CREATE TABLE `Language` (
    `language_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Movie` (
    `movie_id` INT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `duration` INT NOT NULL, 
    `description` TEXT NOT NULL,
    `release_date` DATE NOT NULL,
    `status` VARCHAR(50) NOT NULL, -- ComingSoon, NowShowing, Ended
    `poster_url` VARCHAR(500) NULL,
    `trailer_url` VARCHAR(500) NULL,
    `language_id` INT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`language_id`) REFERENCES `Language`(`language_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `FavoriteMovie` (
    `user_id` INT NOT NULL,
    `movie_id` INT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `movie_id`),
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`),
    FOREIGN KEY (`movie_id`) REFERENCES `Movie`(`movie_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Genre` (
    `genre_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `MovieGenre` (
    `movie_id` INT NOT NULL,
    `genre_id` INT NOT NULL,
    PRIMARY KEY (`movie_id`, `genre_id`),
    FOREIGN KEY (`movie_id`) REFERENCES `Movie`(`movie_id`),
    FOREIGN KEY (`genre_id`) REFERENCES `Genre`(`genre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Hall` (
    `hall_id` INT AUTO_INCREMENT PRIMARY KEY,
    `branch_id` INT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `capacity` INT NOT NULL,
    `hall_type` VARCHAR(50) NOT NULL, 
    `status` VARCHAR(50) NOT NULL,     
    FOREIGN KEY (`branch_id`) REFERENCES `Branch`(`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Seat` (
    `seat_id` INT AUTO_INCREMENT PRIMARY KEY,
    `hall_id` INT NOT NULL,
    `row` VARCHAR(5) NOT NULL,
    `number` INT NOT NULL,
    `seat_type` VARCHAR(50) NOT NULL, 
    `maintenance_status` VARCHAR(50) NOT NULL,
    FOREIGN KEY (`hall_id`) REFERENCES `Hall`(`hall_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `BranchMovie` (
    `branch_id` INT NOT NULL,
    `movie_id` INT NOT NULL,
    `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`branch_id`, `movie_id`),
    FOREIGN KEY (`branch_id`) REFERENCES `Branch`(`branch_id`),
    FOREIGN KEY (`movie_id`) REFERENCES `Movie`(`movie_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================================================
-- 5. PHÂN HỆ: KHUYẾN MÃI & ĐẶT CHỖ
-- ===================================================

CREATE TABLE `Promotion` (
    `promotion_id` INT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `discount_type` VARCHAR(50) NOT NULL, 
    `discount_value` INT NOT NULL,         
    `min_order_value` INT NOT NULL DEFAULT 0, 
    `max_uses` INT NOT NULL,
    `used_count` INT NOT NULL DEFAULT 0,
    `start_date` DATETIME NOT NULL,
    `end_date` DATETIME NOT NULL,
    `is_branch_specific` TINYINT(1) NOT NULL DEFAULT 0,
    `branch_id` INT NULL,
    `status` VARCHAR(50) NOT NULL,
    FOREIGN KEY (`branch_id`) REFERENCES `Branch`(`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Showtime` (
    `showtime_id` INT AUTO_INCREMENT PRIMARY KEY,
    `movie_id` INT NOT NULL,
    `hall_id` INT NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `status` VARCHAR(50) NOT NULL,
    `base_price` INT NOT NULL, 
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`movie_id`) REFERENCES `Movie`(`movie_id`),
    FOREIGN KEY (`hall_id`) REFERENCES `Hall`(`hall_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `SeatReservation` (
    `reservation_id` INT AUTO_INCREMENT PRIMARY KEY,
    `showtime_id` INT NOT NULL,
    `seat_id` INT NOT NULL,
    `user_id` INT NULL,
    `status` VARCHAR(50) NOT NULL, 
    `expires_at` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`showtime_id`) REFERENCES `Showtime`(`showtime_id`),
    FOREIGN KEY (`seat_id`) REFERENCES `Seat`(`seat_id`),
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Booking` (
    `booking_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `showtime_id` INT NULL, 
    `promotion_id` INT NULL,
    `booking_type` VARCHAR(50) NOT NULL, 
    `status` VARCHAR(50) NOT NULL,        
    `total_price` INT NOT NULL DEFAULT 0,  
    `booking_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`),
    FOREIGN KEY (`showtime_id`) REFERENCES `Showtime`(`showtime_id`),
    FOREIGN KEY (`promotion_id`) REFERENCES `Promotion`(`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Ticket` (
    `ticket_id` INT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` INT NOT NULL,
    `seat_id` INT NOT NULL,
    `ticket_type` VARCHAR(50) NOT NULL, 
    `price` INT NOT NULL,                 
    `qr_code` VARCHAR(255) NOT NULL,
    `status` VARCHAR(50) NOT NULL,       
    `validated_at` DATETIME NULL,
    FOREIGN KEY (`booking_id`) REFERENCES `Booking`(`booking_id`),
    FOREIGN KEY (`seat_id`) REFERENCES `Seat`(`seat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Payment` (
    `payment_id` INT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` INT NOT NULL,
    `amount` INT NOT NULL, 
    `payment_method` VARCHAR(50) NOT NULL, 
    `payment_channel` VARCHAR(50) NOT NULL, 
    `status` VARCHAR(50) NOT NULL,         
    `payment_time` DATETIME NULL,
    `transaction_ref` VARCHAR(255) NULL,
    `receipt_url` VARCHAR(500) NULL,
    FOREIGN KEY (`booking_id`) REFERENCES `Booking`(`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================================================
-- 6. PHÂN HỆ: ĐỒ ĂN THỨC UỐNG (FOOD & BEVERAGE)
-- ===================================================

CREATE TABLE `FoodItem` (
    `item_id` INT AUTO_INCREMENT PRIMARY KEY,
    `category_name` VARCHAR(100) NOT NULL, 
    `name` VARCHAR(150) NOT NULL,
    `description` VARCHAR(500) NOT NULL,
    `price` INT NOT NULL, 
    `stock` INT NOT NULL DEFAULT 0,
    `is_available` TINYINT(1) NOT NULL DEFAULT 1,
    `image_url` VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `FoodOrder` (
    `order_id` INT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` INT NOT NULL,
    `status` VARCHAR(50) NOT NULL,        
    `total_amount` INT NOT NULL DEFAULT 0, 
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`booking_id`) REFERENCES `Booking`(`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `FoodOrderItem` (
    `order_id` INT NOT NULL,
    `item_id` INT NOT NULL,
    `quantity` INT NOT NULL,
    `unit_price` INT NOT NULL,             
    PRIMARY KEY (`order_id`, `item_id`),
    FOREIGN KEY (`order_id`) REFERENCES `FoodOrder`(`order_id`),
    FOREIGN KEY (`item_id`) REFERENCES `FoodItem`(`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================================================
-- 7. PHÂN HỆ: TƯƠNG TÁC & THÔNG BÁO
-- ===================================================

CREATE TABLE `Review` (
    `review_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `movie_id` INT NOT NULL,
    `rating` FLOAT NOT NULL,
    `content` TEXT NOT NULL,
    `status` VARCHAR(50) NOT NULL, 
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`),
    FOREIGN KEY (`movie_id`) REFERENCES `Movie`(`movie_id`),
    CONSTRAINT CHK_Review_Rating CHECK (`rating` >= 1.0 AND `rating` <= 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Feedback` (
    `feedback_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `type` VARCHAR(50) NOT NULL,       
    `subject` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `status` VARCHAR(50) NOT NULL,        
    `assigned_staff_id` INT NULL,         
    `staff_notes` TEXT NULL,              
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `resolved_at` DATETIME NULL,
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`),
    FOREIGN KEY (`assigned_staff_id`) REFERENCES `User`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Notification` (
    `notification_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `type` VARCHAR(50) NOT NULL, 
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT NOT NULL,
    `channel` VARCHAR(50) NOT NULL, 
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `User`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
