CREATE TABLE location (
	id SERIAL PRIMARY KEY ,
	x real,
	y real,
	map_id int
);

CREATE TABLE accesspoint (
	id SERIAL PRIMARY KEY,
	mac_addr varchar(18)
);

CREATE TABLE rssi (
	id_loc int REFERENCES location,
	id_ap int REFERENCES accesspoint,
	avg_val real,
	std_dev real
);

CREATE TABLE temprssi (
	ap_id int REFERENCES accesspoint,
	client_mac varchar(18),
	avg_val real
);

CREATE TABLE maps (
	id SERIAL PRIMARY KEY,
	description varchar(100),
	px_width int,
	px_height int,
	meters_width real,
	meters_height real,
	content bytea
);
	
