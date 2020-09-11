def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def message = ''
    def tips = "${config.tips}"

    if (tips == 'null') {
        message = "${config.message}"
    } else{
        message = env."${config.tips}"
    }

    def submitter = "${config.submitter}"
    def timeunit = "${config.timeunit}"
    def timeout = Integer.parseInt(config.timeout)

    // *********下面3项配置需要在系统部署后更改(共3项)************
    // 平台当前环境访问地址-官方网站
    def website = "http://dcfenga.example.com/"
    // 客服电话
    def serviceline = "15829296996"
    // 客服邮箱
    def mailbox = "dcfenga@sina.com"

    if (timeout > 0) {
        timelimit.expire(timeout, timeunit) {
            def inputResponse = input message: message, parameters: [[$class: 'StringParameterDefinition', name: 'opinion', defaultValue: '同意'], [$class: 'StringParameterDefinition', name: 'timeout', defaultValue: timeout + '-' +timeunit], [$class: 'StringParameterDefinition', name: 'website', defaultValue: website], [$class: 'StringParameterDefinition', name: 'serviceline', defaultValue: serviceline], [$class: 'StringParameterDefinition', name: 'mailbox', defaultValue: mailbox]], submitter: submitter

            echo('Approval response:' + inputResponse)
        }
    } else {
        timelimit.expire(24, 'HOURS') {
            def inputResponse = input message: message, parameters: [[$class: 'StringParameterDefinition', name: 'opinion', defaultValue: '同意'], [$class: 'StringParameterDefinition', name: 'timeout', defaultValue: '24-HOURS'], [$class: 'StringParameterDefinition', name: 'website', defaultValue: website], [$class: 'StringParameterDefinition', name: 'serviceline', defaultValue: serviceline], [$class: 'StringParameterDefinition', name: 'mailbox', defaultValue: mailbox]], submitter: submitter

            echo('Approval response:' + inputResponse)
        }
    }
}