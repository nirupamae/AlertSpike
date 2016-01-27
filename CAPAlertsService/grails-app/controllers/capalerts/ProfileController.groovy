package capalerts

import grails.converters.JSON

class ProfileController {

  def ESWrapperService

  def index() { 
    def result = [:]
    result.alerts = AlertProfile.executeQuery('select ap from AlertProfile as ap where ap.name like ?',['%'], [max: 10, offset: 0]);

    withFormat {
      html result
      json { render result as JSON }
    }
  }

  def feed() {

    def result = [:]
    result.offset = 0;
    result.max = 10;

    def esclient = ESWrapperService.getClient();

    if ( ( params.id ) && ( esclient ) ) {
      result.alert = AlertProfile.get(params.id)
      if ( result.alert ) {

        def query_str="*"

        try {

          def search_future = esclient.search {
                       indices 'alerts'
                       types 'alert'
                       source {
                         from = result.offset
                         size = result.max
                         // query {
                         //   query_string (query: query_str)
                         // }

                         query {
                           bool : {
                             must : {
                               match_all : {}
                             }
                             filter : {
                               geo_shape : {
                                 alertShape : {
                                   shape : {
                                     type : "polygon"
                                     coordinates : [ [-109.5297,40.4554], [-109.5298,40.4556], [-109.5299,40.4556], [-109.5299,40.4554], [-109.5297,40.4554] ]
                                   }
                                 }
                               }
                             }
                           }
                         }

                         // facets {
                         //   'Component Type' {
                         //     terms {
                         //       field = grailsApplication.config.globalSearch.typingField
                         //     }
                         //   }
                         // }
                       }
                     }

          def search_response =  search_future.get()
          log.debug("Got response: ${search_response}");
          result.hits = []
          search_response.hits.hits.each { hit ->
            log.debug("Adding hit ${hit}");
            result.hits.add(hit.source)
          }
        }
        catch ( Exception e ) {
          log.error("Error processing search", e);
        }

      }
      else {
        result.message = "Unable to locate profile with id "+params.id;
      }
    }
    else {
    }

    withFormat {
      json { render result as JSON }
      html result
    }

  }

}
