'''
Created on 01/10/2017

@author: Dimitar

#print ('Q61')
#print (model.most_similar(positive=['Q61'], topn=100))
#print('Similarity between Washington D.C. and Los Angeles')
#print(model.similarity("http://www.wikidata.org/entity/Q61", "http://www.wikidata.org/entity/Q65"))
'''
import gensim, logging, os, sys
from gensim.models.keyedvectors import KeyedVectors

from urlparse import urlparse
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import SocketServer


model = gensim.models.Word2Vec.load('../models/wikid2Vec_sg_200_5_5_15_4_500')

class S(BaseHTTPRequestHandler):

	def _set_headers(self):
		self.send_response(200)
		self.send_header('Content-type', 'text/html')
		self.end_headers()

	def do_GET(self):
		self._set_headers()
		query = urlparse(self.path).query
		# length parameter for ignorance of queries with length less than 3
		# fav.ico requests for instance
		if len(query) > 3:
			queryComponents = dict(qc.split("=") for qc in query.split("&"))
			queryNumber = int(queryComponents["query_number"])
			vocab = model.vocab
			output = '[';
			preface = "http://www.wikidata.org/entity/";
			for i in xrange(0, queryNumber):
				queryToRun = queryComponents["query_"+str(i)]
				parts = queryToRun.split(",")
				entity1 = preface + parts[0]
				entity2 = preface + parts[1]
				if all(x in vocab for x in [entity1, entity2]):
					similarity = str(model.similarity(entity1, entity2))
					output += '{"entity1":"'+entity1+\
							'", "entity2":"'+entity2+\
							'", "similarity":'+str(similarity)+'},'
				else:
					output += '{"entity1":"'+entity1+\
							'", "entity2":"'+entity2+\
							'", "similarity":0},'
			output = output[:-1]
			output += ']';
			self.wfile.write(output)
		else:
			self.wfile.write("Bad query.")

	def do_HEAD(self):
		self._set_headers()
        
	def do_POST(self):
		length = int(self.headers.getheader('content-length'))
		query = self.rfile.read(length)
		self._set_headers()
		# length parameter for ignorance of queries with length less than 3
		# fav.ico requests for instance
		if len(query) > 3:
			print(query)
			queryComponents = dict(qc.split("=") for qc in query.split("&"))
			queryNumber = int(queryComponents["query_number"])
			vocab = model.vocab
			output = '[';
			preface = "http://www.wikidata.org/entity/";
			#preface = "";
			for i in xrange(0, queryNumber):
				queryToRun = queryComponents["query_"+str(i)]
				parts = queryToRun.split("%2C")
				if len(parts) > 2:
					l1 = list()
					for index in range(0, len(parts) - 1):
						l1.append(preface + parts[index])
					l2 = list()
					l2.append(preface + parts[len(parts) - 1])
					l_conform = list()
					similarity = 0
					for word in l1:
						try:
							testword = \
								model.__getitem__(word)
							l_conform.append(word)
						except:
							pass

					if len(l_conform):
						try:
							similarity = model.n_similarity(l_conform, l2)
						except Exception as e:
							similarity = 0
					output += '{"entities":['
					for entity in l1:
						output += '"' + entity + '",'
					output += '"' + l2[0] + '"'
					output += '], "similarity":'+str(similarity)+'},'
				else:
					entity1 = preface + parts[0]
					entity2 = preface + parts[1]
					if all(x in vocab for x in [entity1, entity2]):
						similarity = str(model.similarity(entity1, entity2))
						output += '{"entities":["'+entity1+\
								'", "'+entity2+\
								'"], "similarity":'+str(similarity)+'},'
					else:
						output += '{"entities":["'+entity1+\
								'", "'+entity2+\
								'"], "similarity":0},'
			output = output[:-1]
			output += ']';
			self.wfile.write(output)
		else:
			self.wfile.write("Bad query.")
        
def run(server_class=HTTPServer, handler_class=S, port=8801):
	server_address = ('', port)
	httpd = server_class(server_address, handler_class)
	print 'Starting httpd...'
	httpd.serve_forever()

if __name__ == "__main__":
	from sys import argv

	if len(argv) == 2:
		run(port=int(argv[1]))
	else:
		run()
