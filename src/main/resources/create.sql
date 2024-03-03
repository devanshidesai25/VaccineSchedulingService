CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Reservations (
    AppointmentID varchar (255),
    vname varchar(255) REFERENCES Vaccines(name),
    pname varchar(255) REFERENCES Patients(Username),
    cname varchar(255) REFERENCES Caregivers(Username),
	rtime date,
    PRIMARY KEY (AppointmentID)
);
