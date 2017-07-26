function $(s) {
    var sn = s;
    return {
        "invoke":function() {
            var array = [];
            for(var i = 1; i < arguments.length; i++) {
                array.push(arguments[i]);
            }
            return java2js(g_maggieService.invoke(sn, arguments[0], array));
        },
        "declare":function() {
            g_maggieService.declare(arguments[0], arguments[1]);
        }
    };
}

function java2js(java) {
    if(null == java) {
        return null;
    }
    java = '' + java;
    if('true' == java) {
        return true;
    }
    else if('false' == java) {
        return false;
    }
    else if(0 == java.indexOf('\'') && java.length - 1 == java.lastIndexOf('\'')) {
        return java.substring(1, java.length - 1);
    }
    else if((0 == java.indexOf('{') && java.length - 1 == java.lastIndexOf('}')) || (0 == java.indexOf('[') && java.length - 1 == java.lastIndexOf(']'))){
        return eval('(' + java + ')');
    }
    else {
        return parseFloat(java);
    }
}

function json2string(json) {
    if('number' == typeof(json)) {
        return json;
    }
    else if('boolean' == typeof(json)) {
        return json;
    }
    else if('string' == typeof(json)) {
        return '"' + json + '"';
    }
    else if('object' == typeof(json)) {
        if(null == json) {
            return 'null';
        }
        else if(json instanceof Function) {
            return json;
        }
        else if(json instanceof Array) {
            var r = '[';
            for(var i = 0; i < json.length; i++) {
                if(i > 0) {
                    r += ',';
                }
                r += json2string(json[i]);
            }
            r += ']';
            return r;
        }
        else {
            var r = '{';
            var sentry = false;
            for(var i in json) {
                if(sentry) {
                    r += ',';
                }
                else {
                    sentry = true;
                }
                r += '"' + i + '":';
                r += json2string(json[i]);
            }
            r += '}';
            return r;
        }
    }
    else {
        logger.error('unexpected type in json2string(?)');
        return null;
    }
}
