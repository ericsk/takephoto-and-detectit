#r "Newtonsoft.Json"

using System;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using Newtonsoft.Json;

// Get your own FACE API: https://azure.microsoft.com/services/cognitive-services/face/
const string FACEAPI_URL = "YOUR_FACE_API_URL";
const string FACEAPI_KEY = "YOUR_FACE_API_KEY";

public static async Task Run(byte[] photo, string name, IAsyncCollector<FaceResult> outputTable, TraceWriter log)
{
    using (var client = new HttpClient())
    {
        var photoContent = new StreamContent(new MemoryStream(photo));
        client.DefaultRequestHeaders.Add("Ocp-Apim-Subscription-Key", FACEAPI_KEY);
        photoContent.Headers.ContentType = new MediaTypeHeaderValue("application/octet-stream");

        var httpResponse = await client.PostAsync(FACEAPI_URL + "/detect?returnFaceAttributes=age,gender", photoContent);
        if (httpResponse.StatusCode == HttpStatusCode.OK)
        {
            var respBody = await httpResponse.Content.ReadAsStringAsync();

            Face[] faces = JsonConvert.DeserializeObject<Face[]>(respBody);
            foreach (Face face in faces)
            {
                await outputTable.AddAsync(new FaceResult()
                {
                    PartitionKey = "FaceDetection",
                    RowKey =  DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds.ToString(),
                    Age = face.FaceAttributes.Age,
                    Gender = face.FaceAttributes.Gender
                });
            }
        }
    }
}



public class Face
{
    public string FaceId { get; set; }
    public FaceRectangle FaceRectangle { get; set; }
    public FaceAttributes FaceAttributes { get; set; }
}

public class FaceRectangle
{
    public int Top { get; set; }
    public int Left { get; set; }
    public int Width { get; set; }
    public int Height { get; set; }
}

public class FaceAttributes
{
    public double Age { get; set; }
    public string Gender { get; set; }
}

public class FaceResult
{
    public string PartitionKey { get; set; }
    public string RowKey { get; set; }
    public double Age { get; set; }
    public string Gender { get; set; }
}