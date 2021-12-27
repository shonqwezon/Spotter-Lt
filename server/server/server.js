'use strict';
var fs = require('fs');
var multer = require('multer');
const morgan = require('morgan');

var nodemailer = require('nodemailer');
var express = require('express');
var app = express();
var io = require('socket.io')(app.listen(8383));

var bodyParser = require('body-parser');
var urlencodedParser = bodyParser.urlencoded({ extended: false });
var jsonParser = bodyParser.json();

var auth = require('./auth');
var pos = require('./pos');
var profile = require('./profile');

var upload = multer({ dest: "media" });
app.use(express.static(__dirname));
app.use(morgan('dev'));

var fileExt = ".png";
var abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

//registry, returns token + id
app.post('/reg', urlencodedParser, (req, res) => {
    auth.reg(req.body).then((result) => {
        if (result.status == 200) res.sendStatus(result.status);
        else res.status(result.status).end(result.error);
    }).catch((error) => { console.log("Server " + error); });
});


//authentication, returns code
app.post('/auth', urlencodedParser, (req, res) => {
    auth.login(req.body).then((result) => {
        if (result) {
            res.send(result);
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});



app.post('/changePass', urlencodedParser, (req, res) => {
    auth.check(req.headers).then((result) => {
        if (result == true) {
            auth.changePass(req.headers, req.body).then((resolve) => {
                if (resolve) res.sendStatus(200);
                else res.sendStatus(401);
            }).catch((error) => { console.log("Server " + error); });
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});


app.get('/', (req, res) => {
    res.send("Main page");
});



app.post('/restorePass', (req, res) => {
    auth.restorePass(req.headers.login).then((result) => {
        if (typeof result === 'undefined') {
            res.sendStatus(404);
        }
        else {
            var pass = "";
            while (pass.length < 6) {
                pass += abc[Math.floor(Math.random() * abc.length)];
            }
            auth.updateRecoveryPass(req.headers.login, pass);
            var mailOptions = {
                from: "spotter.app@mail.ru", // sender address
                to: result.email, // list of receivers
                subject: 'Password recovery for Spotter', // Subject line
                html: '<b>Your new password is '+ pass +'</b>' // html body
            };
            var mailer = nodemailer.createTransport({
                host: 'smtp.mail.ru',
                port: 465,
                secure: true,
                auth: {
                    user: 'spotter.app@mail.ru',
                    pass: 'infiniti130191zxc'
                },
                tls: {
                    rejectUnauthorized: false
                }
            });
            mailer.sendMail(mailOptions, (error, response) => {
                if (error) {
                    console.log('mail not sent \n', error);
                    res.sendStatus(400);
                }
                else {
                    console.log("Message sent: ", response);
                    res.sendStatus(200);
                }
            }); 
        }
    }).catch((error) => { console.log("Server " + error); });
});



//send locations
app.post('/location.send', jsonParser, (req, res) => {
    auth.check(req.headers).then((result) => {
        if (result == true) {
            var position = `${req.body.lat}, ${req.body.long}`
            pos.addPos(position, req.headers.id);
            console.log("Position is " + position + " of user " + req.headers.id);
            res.sendStatus(200);
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});

//get locations
app.get('/location.get', (req, res) => {
    auth.check(req.headers).then((result) => {
        if (result == true) {
            pos.getPos(req.headers.login).then((result) => {
                res.json(result);
            }).catch((error) => { console.log("Server " + error); });
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});

//get info about user
app.post('/getInfo', (req, res) => {
    auth.check(req.headers).then((result) => {
        if (result == true) {
            profile.getInfo(req.headers.id).then((result) => { res.status(200).json(result); })
                .catch((error) => { console.log("getInfo " + error); });
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});


//upload image
app.post("/avatar", upload.single("avatar"), function (req, res) {
    auth.check(req.headers).then((result) => {
        if (result == true) {
            let filedata = req.file;
            if (!filedata) res.status(400).end('Failed to upload image');
            else {
                fs.renameSync(filedata.path, filedata.path + fileExt, (err) => { if (err) console.log(err) });
                profile.avatarLinkUp(req.headers.id, filedata.path + fileExt);
                res.status(200).end(filedata.path + fileExt);
            }
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});



//get permiss
app.post('/getUsersMain', (req, res) => {
    auth.check(req.headers).then((result) => {
        if (result == true) {
            profile.getUsersMain(req.headers.login).then((res2) => { res.status(200).json(res2); })
                .catch((error) => { console.log("getInfo " + error); });
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});




//get permiss
app.post('/getUsersSub', (req, res) => {
    auth.check(req.headers).then((result) => {
        if (result == true) {
            profile.getUsersSub(req.headers.login).then((res1) => { res.status(200).json(res1); })
                .catch((error) => { console.log("getPermiss " + error); });
        }
        else res.sendStatus(401);
    }).catch((error) => { console.log("Server " + error); });
});






io.on('connection', (socket) => {
    auth.sockSet(socket.handshake.query.IDofUser, socket.id);

    console.log(`New user ${socket.id} has connected`);

    socket.on('disconnect', () => {
        console.log(`User ${socket.id} has disconnected`);
    });
    socket.on("rootID", (data, callback) => {
        console.log("Messgae to " + data.IDsub);
        auth.sockGet(data.IDsub).then((result) => {
            if (result != null) {
                profile.getUsersSub(data.IDmain).then((res1) => {
                    var mass = [];
                    res1.forEach((entry) => {
                        mass.push(entry.user_sub);
                    });
                    console.log(mass.indexOf(data.IDsub) != -1);
                    if (mass.indexOf(data.IDsub) != -1) callback('error_exist');
                    else {
                        io.to(result.socket).emit('message', data.IDmain);
                        callback('OK');
                    }
                });
            } 
            else callback('error_found');
        }).catch((error) => { console.log("Server " + error) });        
    });
    socket.on('responseTo', (data) => {
        console.log(`User ${data.IDmain} is tracking ${data.IDsub}`);
        auth.permissToTrack(data);
    });

    socket.on('deletePermiss', (data) => {
        console.log(`User ${data.IDmain} is not tracking ${data.IDsub}`);
        auth.deleteUsers(data);
    });
});


console.log('Server created on port 8383');
