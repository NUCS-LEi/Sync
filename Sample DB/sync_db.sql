-- ----------------------------
-- Table structure for `example_table`
-- ----------------------------
DROP TABLE IF EXISTS `example_table`;
CREATE TABLE `example_table` (
  `ColumnA` varchar(100) NOT NULL,
  `ColumnB` varchar(100) DEFAULT NULL,
  `LastModDate` char(50) DEFAULT NULL,
  PRIMARY KEY (`ColumnA`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of example_table
-- ----------------------------
INSERT INTO `example_table` VALUES ('RecordA', 'RecordB', '2011-08-30 16:10:10');

-- ----------------------------
-- Table structure for `sync_time`
-- ----------------------------
DROP TABLE IF EXISTS `sync_time`;
CREATE TABLE `sync_time` (
  `PhoneID` char(50) NOT NULL,
  `TableName` char(50) NOT NULL,
  `LastSyncTime` char(30) DEFAULT NULL,
  PRIMARY KEY (`PhoneID`,`TableName`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of sync_time
-- ----------------------------
