#!/usr/bin/env python
# -*- coding: utf-8 -*-
import web
        
urls = (
    '/(.*)', 'hello'
)
app = web.application(urls, globals())

class hello:        
    def GET(self, name):
        if not name: 
            name = 'World'
        return """
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<meta name="google-site-verification" content="bshwBZaBwT5aAoQlK-Krt6HlZyJYcJ_UT02LCPkUaSs" />
</head>
<body>
<p>Hello, """ + name + """!!</p>
<p><a href="static/dbcourse/">слайды лекций</a></p>
</body></html>'
"""
#        return 'Hello, ' + name + '!'

if __name__ == "__main__":
    app.run()
