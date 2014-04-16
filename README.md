# DocRaptor Groovy

This plugin provides a `docraptor.DocRaptor` class for generating PDFs via the [DocRaptor.com](www.docraptor.com) api.

## Usage

You can simply create a new instance of the DocRaptor class with the following options to get your pdf.

```groovy
import docraptor.DocRaptor

def doc = new DocRaptor(apiKey: 'YOUR KEY HERE', documentUrl: 'http://www.example.org/mypage.html', strict:false, documentType: 'pdf')

def myPdf = new File('test.pdf')
myPdf.bytes = doc.bytes
```

Or for asynchronous job processing: 


```groovy
import docraptor.DocRaptor

def doc = new DocRaptor(apiKey: 'YOUR KEY HERE', documentUrl: 'http://www.example.org/mypage.html', strict:false, documentType: 'pdf', async: true)


while(doc.status != 'completed') {
	sleep(1000)
}


def myPdf = new File('test.pdf')
myPdf.bytes = doc.bytes	

```

This plugin supports other DocRaptor api Parameters as specified by their docs, which can be seen [here](https://docraptor.com/documentation)