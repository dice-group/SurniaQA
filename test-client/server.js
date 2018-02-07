const qald = require('./qald-7-train-multilingual.json')
const readline = require('readline')
const request = require('request')
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
})

let index = 0

rl.on('line', (line) => {
  let questionIndex = parseInt(line)
  if (typeof questionIndex === 'number' && questionIndex > 0 && questionIndex < qald.questions.length) {
    queryQuestion(questionIndex)
  } else {
    console.log('Wrong index entered - iterating through questions')
    queryQuestion(index++)
  }

})

function queryQuestion (index) {
  if (index < qald.questions.length) {
    const question = qald.questions[index++]
    const questionString = question.question.filter(e => e.language === 'en')[0].string
    request(
      {
        method: 'POST',
        uri: 'http://localhost:8181/ask-gerbil',
        qs: {
          query: questionString,
          lang: 'en'
        }
      },
      function (error, response, body) {
        if (error) {
          console.log('Error: ' + error)
        } else {
          const result = JSON.parse(body).questions[0]
          console.log('ID: ' + question.id)
          console.log('Question: ' + questionString)
          console.log('\nCorrect query: ' + question.query.sparql)
          console.log('Correct answer: ' + JSON.stringify(question.answers, null, 4))
          console.log('\nSystem query: ' + result.query.sparql)
          console.log('System answer: ' + JSON.stringify(result.answers, null, 4))
          console.log('\n****************************************************************************\n')
        }
      }
    )
  } else {
    console.log('No questions remaining')
  }
}
