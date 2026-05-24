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