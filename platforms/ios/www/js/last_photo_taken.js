var LastPhotoTaken = {
    getLastPhoto: function(success, failure){
        cordova.exec(success, failure, "LastPhotoTaken", "getLastPhoto", []);
    }
};

module.exports = LastPhotoTaken;