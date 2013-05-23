<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page session="false" %>
<html>
<head>
	<title>Home</title>
</head>
<body>
<h1>
	Welcome to My Bite Social
</h1>


<c:forEach items="${notes}" var="note">
	<hr>
    ${note}
</c:forEach>
</body>
</html>
