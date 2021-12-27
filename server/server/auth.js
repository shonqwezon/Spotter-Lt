var pos = require('./pos');
var md5 = require('md5');
var pg = require('pg');
var check = null;


var pool = new pg.Pool({
    user: 'qishon',
    host: '127.0.0.1',
    database: 'gpsdata',
    password: 'infiniti130191',
    port: '5432'
});

module.exports.reg = function (data) {
    return new Promise(function (resolve, reject) {

        var token = md5(data.email + data.login + data.pass);
        var strValue = "INSERT INTO authentication(token, login, pass, email) " + "VALUES('" + token + "', '" + data.login + "', '" + data.pass + "', '" + data.email + "')";

        pool.query(strValue, (err) => {
            if (err) {
                if (err.code == '23505') {
                    pool.query("select nextval('authentication_id_seq');", (err, result) => {
                        if (err) console.log(err);
                        pool.query(`ALTER SEQUENCE authentication_id_seq restart WITH ${result.rows[0].nextval - 1};`, (err) => { if (err) console.log(err); });
                    });

                    resolve({ "status": 400, "error": err.constraint });
                }
                else {
                    console.log("Pool " + err);
                    pool.end();
                    reject(err);
                }
            }
            else {
                pool.query(`SELECT id FROM authentication WHERE token = '${token}';`, (err, result1) => {
                    if (err) {
                        console.log("Pool " + err);
                        pool.end();
                        reject(err);
                    }
                    pos.addPos(`0, 0`, result1.rows[0].id);
                });
                resolve({ "status": 200 });
            }
        });
    });
};


module.exports.check = function (info) {
    return new Promise(function (resolve, reject) {
        pool.query(`SELECT token FROM authentication WHERE id = ${info.id};`, (err, result) => {
            if (err) {
                console.log("Pool " + err)
                pool.end();
                reject(err);
            }
            if (result.rows[0].token == info.key) check = true;
            else check = false;
            resolve(check);
        });
    });
};



module.exports.sockSet = function (userId, socketId) {
    pool.query("UPDATE authentication SET socket = '" + socketId + `' WHERE id = ${userId};`, (err) => {
        if (err) {
            console.log("Pool " + err);
            pool.end();
        }
    });
};



module.exports.sockGet = function (userId) {
    return new Promise(function (resolve, reject) {
        pool.query(`SELECT socket FROM authentication WHERE login = '${userId}';`, (err, result) => {
            if (err) {
                console.log("Pool " + err);
                pool.end();
                reject(err);
            }
            resolve(result.rows[0]); 
        });
    });
};


module.exports.permissToTrack = function (data) {
    var strValue = `INSERT INTO permisstrack(user_main, user_sub) ` + "VALUES('" + data.IDmain + "', '" + data.IDsub + "');";
    pool.query(strValue, (err) => {
        if (err) {
            console.log("Pool " + err);
            pool.end();
        }
    });
};


module.exports.deleteUsers = function (login) {
    pool.query(`DELETE FROM permisstrack WHERE user_main = '${login.IDmain}' AND user_sub = '${login.IDsub}';`, (err) => {
        if (err) {
            console.log("Pool " + err)
            pool.end();
        }
    });
};



module.exports.changePass = function (info, body) {
    return new Promise(function (resolve, reject) {
        pool.query(`SELECT * FROM authentication WHERE id = '${info.id}';`, (err, result) => {
            if (err) {
                console.log("Pool " + err);
                pool.end();
                reject(err);
            }
            if (result.rows[0].pass == body.oldPass) {
                var token = md5(result.rows[0].email + result.rows[0].login + body.newPass);
                pool.query("UPDATE authentication SET token = '" + token + `' WHERE id = ${info.id};`, (err) => {
                    if (err) {
                        console.log("Pool " + err);
                        pool.end();
                    }
                });
                pool.query("UPDATE authentication SET pass = '" + body.newPass + `' WHERE id = ${info.id};`, (err) => {
                    if (err) {
                        console.log("Pool " + err);
                        pool.end();
                    }
                });
                resolve(true);
            }
            else resolve(false);
        });
    });
};


module.exports.updateRecoveryPass = function (login, pass) {
    pool.query(`SELECT * FROM authentication WHERE login = '${login}';`, (err, result) => {
        if (err) {
            console.log("Pool " + err);
            pool.end();
            reject(err);
        }
        var token = md5(result.rows[0].email + login + pass);
        pool.query("UPDATE authentication SET token = '" + token + `' WHERE login = '${login}';`, (err) => {
            if (err) {
                console.log("Pool " + err);
                pool.end();
            }
        });
        pool.query("UPDATE authentication SET pass = '" + pass + `' WHERE login = '${login}';`, (err) => {
            if (err) {
                console.log("Pool " + err);
                pool.end();
            }
        });
    });
};



module.exports.restorePass = function (login) {
    return new Promise(function (resolve, reject) {
        pool.query(`SELECT email FROM authentication WHERE login = '${login}';`, (err, result) => {
            if (err) {
                console.log("Pool " + err);
                pool.end();
                reject(err);
            }
            console.log(result.rows[0]);
            resolve(result.rows[0]);
        });
    });
};



module.exports.login = function (data) {
    return new Promise(function (resolve, reject) {
        pool.query("SELECT pass FROM authentication WHERE login = " + "'" + data.login + "';", (err, result) => {
            if (err) {
                console.log("Pool " + err);
                pool.end();
                reject(err);
            }
            var pass = { pass: `${data.pass}` };
            if (JSON.stringify(result.rows[0]) == JSON.stringify(pass)) {
                pool.query("SELECT * FROM authentication WHERE login = " + "'" + data.login + "';", (err, result) => {
                    if (err) {
                        console.log("Pool " + err);
                        pool.end();
                        resolve(err);
                    }
                    else {
                        var serial = { "id": `${result.rows[0].id}`, "token": `${result.rows[0].token}` };
                        resolve(serial);
                    }
                });
            }
            else {
                check = false;
                resolve(check);
            }
        });
    });
};

