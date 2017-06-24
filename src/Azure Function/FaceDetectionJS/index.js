let request = require('request');

// Get your own FACE API: https://azure.microsoft.com/services/cognitive-services/face/
const FACEAPI_URL = "YOUR_FACE_API_URL";
const FACEAPI_KEY = "YOUR_FACE_API_KEY";

module.exports = function (context, photo) {

    let postOpt = {
        url: FACEAPI_URL + "?returnFaceAttributes=age,gender",
        body: photo,
        method: 'POST',
        headers: {
            "Ocp-Apim-Subscription-Key": FACEAPI_KEY,
            "Content-Type": "application/octet-stream"
        }
    };
    request(postOpt, function(error, response, body) {
        context.bindings.outputTable = [];
        
        let faces = JSON.parse(body);
        for (let i in faces) {
            let face = faces[i];
            context.bindings.outputTable.push({
                "PartitionKey": "FaceAPIDemo",
                "RowKey": (new Date()).getTime(),
                "Age": face.faceAttributes.age,
                "Gender": face.faceAttributes.gender
            });
        }

        context.done();
    });
};