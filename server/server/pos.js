var pg = require('pg');
var profile = require('./profile');

var pool = new pg.Pool({
    user: 'qishon',
    host: '127.0.0.1',
    database: 'gpsdata',
    password: 'infiniti130191',
    port: '5432'
});

module.exports.addPos = function (coord, id) {
    var strValue = `INSERT INTO locations(user_id, pos, datetime) ` + "VALUES('" + id + "', '" + coord + "', now());";
    pool.query(strValue, (err, result) => {
        if (err) {
            console.log("Pool " + +err);
            pool.end();
        }
    });
};

module.exports.getPos = function (login) {
    return new Promise(function (resolve, reject) {
        var mass = [];
        profile.getUsersSub(login).then((res1) => {
            new Promise(function (res, rej) {
                var i = 0;
                for (let user of res1) {
                    pool.query(`SELECT id FROM authentication WHERE login = '${user.user_sub}';`, (err, result) => {
                        if (err) {
                            console.log("Pool " + err)
                            pool.end();
                            reject(err);
                        }
                        pool.query(`SELECT * FROM locations WHERE user_id = ${result.rows[0].id} ORDER BY id DESC LIMIT 1;`, (err1, result1) => {
                            if (err1) {
                                console.log("Pool " + err1)
                                pool.end();
                                reject(err1);
                            }
                            var position = {
                                user: user.user_sub,
                                lat: result1.rows[0].pos.x,
                                long: result1.rows[0].pos.y,
                                datetime: result1.rows[0].datetime
                            };
                            mass.push(position);
                            i++;
                            if (i == res1.length) res(mass);
                        });
                    });
                }
            }).then((res) => {
                console.log("complete");
                resolve(res);
            }).catch((error) => { console.log("Pos " + error); });

        }).catch((error) => { console.log("getPermiss " + error); });
    });
};
