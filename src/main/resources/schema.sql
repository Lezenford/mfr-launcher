CREATE TABLE IF NOT EXISTS PROPERTY (
  ID    INT PRIMARY KEY AUTO_INCREMENT,
  KEY   VARCHAR2(MAX) UNIQUE NOT NULL,
  VALUE VARCHAR(MAX)
);
CREATE TABLE IF NOT EXISTS PLUGIN_GROUP (
  ID     INT PRIMARY KEY AUTO_INCREMENT,
  VALUE  VARCHAR2(MAX) UNIQUE NOT NULL,
  ACTIVE BOOLEAN DEFAULT TRUE NOT NULL
);
CREATE TABLE IF NOT EXISTS RELEASE (
  ID              INT PRIMARY KEY AUTO_INCREMENT,
  PLUGIN_GROUP_ID INT                   NOT NULL,
  VALUE           VARCHAR2(MAX)         NOT NULL,
  DEFAULT         BOOLEAN DEFAULT TRUE  NOT NULL,
  APPLIED         BOOLEAN DEFAULT FALSE NOT NULL,
  IMAGE_PATH      VARCHAR2(MAX),
  DESCRIPTION     VARCHAR2(MAX),
  ACTIVE          BOOLEAN DEFAULT TRUE  NOT NULL,
  FOREIGN KEY (PLUGIN_GROUP_ID) REFERENCES PLUGIN_GROUP (ID)
);
CREATE UNIQUE INDEX 'RELEASE_UNIQUE_INDEX'
  ON RELEASE (PLUGIN_GROUP_ID, VALUE);
CREATE TABLE IF NOT EXISTS DETAILS (
  ID           INT PRIMARY KEY AUTO_INCREMENT,
  RELEASE_ID   INT                  NOT NULL,
  STORAGE_PATH VARCHAR2(MAX)        NOT NULL,
  GAME_PATH    VARCHAR2(MAX)        NOT NULL,
  ACTIVE       BOOLEAN DEFAULT TRUE NOT NULL,
  MD5          BINARY,
  FOREIGN KEY (RELEASE_ID) REFERENCES RELEASE (ID)
);
CREATE UNIQUE INDEX 'DETAILS_UNIQUE_INDEX'
  ON DETAILS (RELEASE_ID, STORAGE_PATH);
