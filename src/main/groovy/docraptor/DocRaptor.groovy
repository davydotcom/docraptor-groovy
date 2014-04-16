package docraptor

import groovy.util.logging.Log4j
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.*


/**
* An interface class for making API Requests via DocRaptor for PDF Files.
* @author David Estes
*/
@Log4j
class DocRaptor {
	static defaultApiKey

	def apiKey
	def requestOptions = [
	 tag: 'Grails',
	 documentType: 'pdf',
	 name: null,
	 test: false,
	 documentUrl: null,
	 documentContent: null,
	 async: false,
	 strict: null,
	 javascript: false
	]

	def ssl = false
	def statusCode
	def statusId
	def status
	Boolean loaded = false
	Byte[] bytes

	DocRaptor(Map options) {
		ssl = options.remove('ssl')
		apiKey = options.remove('apiKey')
		requestOptions = (requestOptions + options).findAll{ it.value != null }
		apiKey = apiKey ?: defaultApiKey

		requestDocument()
	}

	def requestDocument() {
		def http = new HTTPBuilder( ssl ? 'https://docraptor.com/docs' : 'http://docraptor.com/docs' )

		http.request(POST) {
			uri.query = [user_credentials: apiKey]
			def documentParams = requestOptions.collectEntries { optionKey, optionValue ->
				def key = underscoreString(optionKey)
				["doc[${key}]", optionValue]
			}
			
			send URLENC, documentParams
		
			response.success = { resp, reader ->
				statusCode = 200
				if(requestOptions.async) {
					statusId = reader.status_id
				} else {
					bytes = reader.bytes
					loaded = true
				}
			}

			response.'400' = {
				statusCode = 400
				log.error = "Something Went wrong with your request. Please validate your request parameters and try again - ${requestOptions}"
			}

			response.'401' = {
				statusCode = 401
				log.error = "Invalid Credentials Provided For Creating a DocRaptor Document."
			}

			response.'403' = {
				statusCode = 403
				log.error = 'Forbidden - You are potentially making too many document requests to DocRaptor.'
			}

			response.'422' = {
				statusCode = 422
				log.error = 'Failure Processing DocRaptor Document, Please try turning off strict HTML validation mode.'
			}

			response.failure = { resp ->
				statusCode = resp.status
				log.error = "DocRaptor Request Failed Code ${statusCode}"
			}
		}

	}

	def getStatus() {
		if(loaded) {
			return status ?: "completed"
		} else if(statusId) {
			// We can check the status and download it if its completed
			def http = new HTTPBuilder( (ssl ? 'https://docraptor.com/status/' : 'http://docraptor.com/status/') + statusId )
			http.request(GET) {
				response.success = { resp, json ->
					status = json.status
					if(json.download_url) {
						loadDocumentFromURL(json.download_url)
					}
				}
			}
		}

		return status
	}

	private loadDocumentFromURL(url) {
		def http = new HTTPBuilder(url)

		http.request(GET) {
			response.success = { resp, reader ->
				bytes = reader.bytes
				loaded = true
			}
			response.failure = { 
				status = "Download Error"
				log.error = "Error Downloading Document For ${requestOptions?.name} -- ${url}!"
			}
		}
	}

	private underscoreString(String input) {
		def output = input.replaceAll("-","_")
        output.replaceAll(/\B[A-Z]/) { '_' + it }.toLowerCase()
        return output
	}
}